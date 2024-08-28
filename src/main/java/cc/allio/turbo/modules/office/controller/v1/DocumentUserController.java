package cc.allio.turbo.modules.office.controller.v1;

import cc.allio.turbo.common.exception.BizException;
import cc.allio.turbo.common.web.R;
import cc.allio.turbo.modules.auth.provider.TurboUser;
import cc.allio.turbo.modules.office.dto.DocumentDTO;
import cc.allio.turbo.modules.office.dto.OnlineDocUser;
import cc.allio.turbo.modules.office.dto.page.DocPageDTO;
import cc.allio.turbo.modules.office.service.IDocUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
@RequestMapping("/office/v1/doc/user")
@AllArgsConstructor
@Tag(name = "用户文档", description = "用户文档")
public class DocumentUserController {

    private final IDocUserService docUserService;

    @PutMapping("/favor/{docId}")
    @Operation(summary = "固定常用的文档")
    public R<Boolean> favorOfDocument(@Valid @NotNull TurboUser currentUser, @NotNull @PathVariable("docId") Long docId) throws BizException {
        Boolean success = docUserService.favorOfDocument(currentUser, docId);
        return R.ok(success);
    }

    @PutMapping("/favor/cancel/{docId}")
    @Operation(summary = "取消固定常用的文档")
    public R<Boolean> cancelFavorOfDocument(@Valid @NotNull TurboUser currentUser, @NotNull @PathVariable("docId") Long docId) throws BizException {
        Boolean success = docUserService.cancelFavorOfDocument(currentUser, docId);
        return R.ok(success);
    }

    @PutMapping("/favorite/{docId}")
    @Operation(summary = "收藏指定的文档")
    public R<Boolean> favoriteOfDocument(@Valid @NotNull TurboUser currentUser, @NotNull @PathVariable("docId") Long docId) throws BizException {
        Boolean success = docUserService.favoriteOfDocument(currentUser, docId);
        return R.ok(success);
    }

    @PutMapping("/favorite/cancel/{docId}")
    @Operation(summary = "取消收藏指定的文档")
    public R<Boolean> cancelFavoriteDocument(@Valid @NotNull TurboUser currentUser, @NotNull @PathVariable("docId") Long docId) throws BizException {
        Boolean success = docUserService.cancelFavoriteOfDocument(currentUser, docId);
        return R.ok(success);
    }

    @GetMapping("/searchMine")
    @Operation(summary = "搜索我的文档")
    public R<List<DocumentDTO>> searchMineDocument(@Valid @NotNull TurboUser currentUser, @NotNull String pattern) throws BizException {
        List<DocumentDTO> documentList = docUserService.searchMineDocument(currentUser, pattern);
        return R.ok(documentList);
    }

    @PostMapping("/getMine")
    @Operation(summary = "获取我的文档列表")
    public R<IPage<DocumentDTO>> getMineDocument(@Valid @NotNull TurboUser currentUser, @RequestBody DocPageDTO params) throws BizException {
        IPage<DocumentDTO> documentList = docUserService.selectUserDocument(currentUser, params);
        return R.ok(documentList);
    }

    @PostMapping("/getMineRecently")
    @Operation(summary = "获取最近我的文档列表")
    public R<IPage<DocumentDTO>> getMineRecentlyDocument(@Valid @NotNull TurboUser currentUser, @RequestBody DocPageDTO params) throws BizException {
        IPage<DocumentDTO> documentList = docUserService.selectRecentlyDocument(currentUser, params);
        return R.ok(documentList);
    }

    @PostMapping("/getShareToMe")
    @Operation(summary = "获取分享给我的文档列表")
    public R<IPage<DocumentDTO>> getShareToMeDocument(@Valid @NotNull TurboUser currentUser, @RequestBody DocPageDTO params) throws BizException {
        IPage<DocumentDTO> documentList = docUserService.selectShareToMeDocument(currentUser, params);
        return R.ok(documentList);
    }

    @PostMapping("/getMineFavorite")
    @Operation(summary = "获取我喜爱文档列表")
    public R<IPage<DocumentDTO>> getMineFavoriteDocument(@Valid @NotNull TurboUser currentUser, @RequestBody DocPageDTO params) throws BizException {
        IPage<DocumentDTO> documentList = docUserService.selectMineFavoriteDocument(currentUser, params);
        return R.ok(documentList);
    }

    @PostMapping("/getMineCreate")
    @Operation(summary = "获取我创建文档列表")
    public R<IPage<DocumentDTO>> getMineCreateDocument(@Valid @NotNull TurboUser currentUser, @RequestBody DocPageDTO params) throws BizException {
        IPage<DocumentDTO> documentList = docUserService.selectMineCreateDocument(currentUser, params);
        return R.ok(documentList);
    }

    @PostMapping("/getMineFavor")
    @Operation(summary = "获取我常用文档列表")
    public R<IPage<DocumentDTO>> getMineFavorDocument(@Valid @NotNull TurboUser currentUser, @RequestBody DocPageDTO params) throws BizException {
        IPage<DocumentDTO> documentList = docUserService.selectMineFavorDocument(currentUser, params);
        return R.ok(documentList);
    }

    @PostMapping("/kickout/{docId}")
    @Operation(summary = "踢出指定人")
    public R<Boolean> kickout(@PathVariable("docId") @NotNull Long docId, @RequestBody List<Long> userId) throws BizException {
        Boolean success = docUserService.kickout(docId, userId);
        return R.ok(success);
    }

    @PostMapping("/kickoutOthers/{docId}")
    @Operation(summary = "提出非创建者其他人")
    public R<Boolean> kickoutOthers(@Valid @NotNull TurboUser currentUser, @PathVariable("docId") @NotNull Long docId) throws BizException {
        Boolean success = docUserService.kickoutOthres(currentUser, docId);
        return R.ok(success);
    }

    @PostMapping("/kickoutAll/{docId}")
    @Operation(summary = "踢出所有人")
    public R<Boolean> kickoutAll(@PathVariable("docId") @NotNull Long docId) throws BizException {
        Boolean success = docUserService.kickoutAll(docId);
        return R.ok(success);
    }

    @GetMapping("/getOnlineDocUser/{docId}")
    @Operation(summary = "获取文档在线编辑人列表")
    public R<List<OnlineDocUser>> getOnlineDocUser(@Valid @NotNull TurboUser currentUser, @PathVariable("docId") @NotNull Long docId) throws BizException {
        List<OnlineDocUser> onlineDocUser = docUserService.getOnlineDocUser(currentUser, docId);
        return R.ok(onlineDocUser);
    }
}
