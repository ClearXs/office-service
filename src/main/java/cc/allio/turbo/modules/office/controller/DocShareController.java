package cc.allio.turbo.modules.office.controller;

import cc.allio.turbo.common.exception.BizException;
import cc.allio.turbo.common.util.RedisUtil;
import cc.allio.turbo.common.util.SecureUtil;
import cc.allio.turbo.common.web.R;
import cc.allio.turbo.modules.auth.provider.TurboUser;
import cc.allio.turbo.modules.office.documentserver.vo.HistoryData;
import cc.allio.turbo.modules.office.documentserver.vo.HistoryList;
import cc.allio.turbo.modules.office.documentserver.vo.Rename;
import cc.allio.turbo.modules.office.dto.OnlineDocUser;
import cc.allio.turbo.modules.office.dto.PermissionShareDTO;
import cc.allio.turbo.modules.office.service.IDocService;
import cc.allio.turbo.modules.office.service.IDocUserService;
import cc.allio.uno.core.util.JsonUtils;
import cc.allio.uno.core.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/office/doc/share")
@AllArgsConstructor
@Tag(name = "文档分享")
public class DocShareController {

    private final IDocUserService docUserService;
    private final IDocService docService;

    @GetMapping("/forceSave/{secureKey}")
    @Operation(summary = "强制保存")
    public R<Boolean> forceSave(@Valid @NotNull TurboUser currentUser, @PathVariable("secureKey") String secureKey) throws BizException {
        Long docId = getDocIdFromSecureKey(secureKey);
        Boolean success = this.docUserService.forceSave(currentUser, docId);
        return R.ok(success);
    }

    @PutMapping("/rename/{docId}")
    @Operation(summary = "修改文档名称")
    public R<Boolean> rename(@PathVariable("docId") Long docId, @Valid @RequestBody Rename rename) throws BizException {
        Boolean success = this.docService.rename(docId, rename);
        return R.ok(success);
    }

    @PutMapping("/restore/{secureKey}/{version}")
    @Operation(summary = "还原指定版本版本")
    public R<Boolean> restore(@PathVariable("secureKey") String secureKey, @PathVariable("version") Integer version) throws BizException {
        Long docId = getDocIdFromSecureKey(secureKey);
        Boolean success = this.docService.restore(docId, version);
        return R.ok(success);
    }

    @GetMapping("/history/{secureKey}")
    @Operation(summary = "获取文档历史版本")
    public R<HistoryList> history(@PathVariable("secureKey") String secureKey) throws BizException {
        Long docId = getDocIdFromSecureKey(secureKey);
        HistoryList history = this.docService.getHistoryList(docId);
        return R.ok(history);
    }

    @GetMapping("/historyData/{secureKey}/{version}")
    @Operation(summary = "获取文档历史版本数据")
    public R<HistoryData> historyData(@PathVariable("secureKey") String secureKey, @PathVariable("version") Integer version) throws BizException {
        Long docId = getDocIdFromSecureKey(secureKey);
        HistoryData historyData = this.docService.getHistoryData(docId, version);
        return R.ok(historyData);
    }

    @PostMapping("/kickout/{secureKey}")
    @Operation(summary = "踢出指定人")
    public R<Boolean> kickout(@PathVariable("secureKey") String secureKey, @RequestBody List<Long> userId) throws BizException {
        Long docId = getDocIdFromSecureKey(secureKey);
        Boolean success = docUserService.kickout(docId, userId);
        return R.ok(success);
    }

    @PostMapping("/kickoutOthers/{secureKey}")
    @Operation(summary = "提出非创建者其他人")
    public R<Boolean> kickoutOthers(@Valid @NotNull TurboUser currentUser, @PathVariable("secureKey") String secureKey) throws BizException {
        Long docId = getDocIdFromSecureKey(secureKey);
        Boolean success = docUserService.kickoutOthres(currentUser, docId);
        return R.ok(success);
    }

    @PostMapping("/kickoutAll/{secureKey}")
    @Operation(summary = "踢出所有人")
    public R<Boolean> kickoutAll(@PathVariable("secureKey") String secureKey) throws BizException {
        Long docId = getDocIdFromSecureKey(secureKey);
        Boolean success = docUserService.kickoutAll(docId);
        return R.ok(success);
    }

    @GetMapping("/getOnlineDocUser/{secureKey}")
    @Operation(summary = "获取文档在线编辑人列表")
    public R<List<OnlineDocUser>> getOnlineDocUser(@Valid @NotNull TurboUser currentUser, @PathVariable("secureKey") String secureKey) throws BizException {
        Long docId = getDocIdFromSecureKey(secureKey);
        List<OnlineDocUser> onlineDocUser = docUserService.getOnlineDocUser(currentUser, docId);
        return R.ok(onlineDocUser);
    }

    /**
     * decrypt secure key and from in cache load doc id
     *
     * @param secureKey share secure key
     * @return doc id
     */
    Long getDocIdFromSecureKey(String secureKey) throws BizException {
        String cacheKey = SecureUtil.getSystemSecureCipher().decrypt(secureKey);
        PermissionShareDTO share = null;
        try {
            String shareString = RedisUtil.get(cacheKey);
            if (StringUtils.isBlank(secureKey)) {
                throw new BizException("share key not found!");
            }
            share = JsonUtils.parse(shareString, PermissionShareDTO.class);
        } catch (Throwable ex) {
            log.error("failed to parse PermissionShare", ex);
            throw new BizException("share key not found!");
        }
        return share.getDocId();
    }
}
