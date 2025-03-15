package cc.allio.turbo.modules.office.service.impl;

import cc.allio.turbo.common.exception.BizException;
import cc.allio.turbo.common.util.AuthUtil;
import cc.allio.turbo.modules.auth.provider.TurboUser;
import cc.allio.turbo.modules.office.constant.ShareMode;
import cc.allio.turbo.modules.office.documentserver.callbacks.CallbackBus;
import cc.allio.turbo.modules.office.documentserver.command.CommandManager;
import cc.allio.turbo.modules.office.documentserver.command.Result;
import cc.allio.turbo.modules.office.documentserver.command.ResultCode;
import cc.allio.turbo.modules.office.documentserver.command.requestinfo.DropArgs;
import cc.allio.turbo.modules.office.documentserver.command.requestinfo.EmptyArgs;
import cc.allio.turbo.modules.office.documentserver.command.requestinfo.ForceSaveArgs;
import cc.allio.turbo.modules.office.documentserver.vo.Track;
import cc.allio.turbo.modules.office.dto.DocumentDTO;
import cc.allio.turbo.modules.office.dto.OnlineDocUser;
import cc.allio.turbo.modules.office.dto.PermissionShareDTO;
import cc.allio.turbo.modules.office.dto.page.DocPageDTO;
import cc.allio.turbo.modules.office.entity.Doc;
import cc.allio.turbo.modules.office.entity.DocCooperator;
import cc.allio.turbo.modules.office.entity.DocCustomization;
import cc.allio.turbo.modules.office.entity.DocPermissionGroup;
import cc.allio.turbo.modules.office.mapper.DocUserMapper;
import cc.allio.turbo.modules.office.service.*;
import cc.allio.turbo.modules.office.vo.DocPermission;
import cc.allio.turbo.modules.office.vo.DocUser;
import cc.allio.turbo.modules.office.vo.DocVO;
import cc.allio.turbo.modules.system.entity.SysUser;
import cc.allio.turbo.modules.system.service.ISysUserService;
import cc.allio.uno.core.util.CollectionUtils;
import cc.allio.uno.core.util.JsonUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class DocUserServiceImpl implements IDocUserService {

    private final IDocService docService;
    private final IDocPermissionGroupService permissionGroupService;
    private final IDocCooperatorService cooperatorService;
    private final IDocCustomizationService docCustomizationService;
    private final ISysUserService sysUserService;
    private final DocUserMapper docUserMapper;

    private final CommandManager commandManager;

    @Override
    public Boolean favoriteOfDocument(TurboUser currentUser, Long docId) throws BizException {
        String userId = currentUser.getUserId();
        return
                docCustomizationService.update(
                        new DocCustomization(),
                        Wrappers.<DocCustomization>lambdaUpdate()
                                .set(DocCustomization::getFavorite, true)
                                .eq(DocCustomization::getUserId, userId)
                                .eq(DocCustomization::getDocId, docId)
                );
    }

    @Override
    public Boolean cancelFavoriteOfDocument(TurboUser currentUser, Long docId) throws BizException {
        String userId = currentUser.getUserId();
        return docCustomizationService.update(
                new DocCustomization(),
                Wrappers.<DocCustomization>lambdaUpdate()
                        .set(DocCustomization::getFavorite, false)
                        .eq(DocCustomization::getUserId, userId)
                        .eq(DocCustomization::getDocId, docId)
        );
    }

    @Override
    public Boolean favorOfDocument(TurboUser currentUser, Long docId) throws BizException {
        String userId = currentUser.getUserId();
        return docCustomizationService.update(
                new DocCustomization(),
                Wrappers.<DocCustomization>lambdaUpdate()
                        .set(DocCustomization::getFavor, true)
                        .eq(DocCustomization::getUserId, userId)
                        .eq(DocCustomization::getDocId, docId)
        );
    }

    @Override
    public Boolean cancelFavorOfDocument(TurboUser currentUser, Long docId) throws BizException {
        String userId = currentUser.getUserId();
        return docCustomizationService.update(
                new DocCustomization(),
                Wrappers.<DocCustomization>lambdaUpdate()
                        .set(DocCustomization::getFavor, false)
                        .eq(DocCustomization::getUserId, userId)
                        .eq(DocCustomization::getDocId, docId)
        );
    }

    @Override
    @Transactional
    public DocUser getDocUserByCurrentUser(TurboUser currentUser, Long docId) throws BizException {
        String userId = currentUser.getUserId();
        String userName = currentUser.getUsername();

        DocUser docUser = new DocUser();
        docUser.setUserId(userId);
        docUser.setUsername(userName);
        Doc doc = docService.getById(docId);
        if (doc == null) {
            throw new BizException("not found document");
        }
        docUser.setDoc(doc);
        DocCooperator docCooperator =
                cooperatorService.getOne(Wrappers.<DocCooperator>lambdaQuery().eq(DocCooperator::getDocId, doc.getId()).eq(DocCooperator::getCooperator, userId));
        DocPermissionGroup permissionGroup;
        if (docCooperator == null) {
            // get default permission group from doc
            permissionGroup = permissionGroupService.selectDefaultPermissionGroup(docId);
            docCooperator = new DocCooperator();
            docCooperator.setDocId(docId);
            docCooperator.setCooperator(userId);
            docCooperator.setPermissionGroupId(permissionGroup.getId());
            // saves to new doc cooperator.
            cooperatorService.save(docCooperator);
        } else {
            permissionGroup = permissionGroupService.getById(docCooperator.getPermissionGroupId());
        }
        // set
        if (permissionGroup != null) {
            String permissionJson = permissionGroup.getPermission();
            DocPermission permission = JsonUtils.parse(permissionJson, DocPermission.class);
            docUser.setPermission(permission);
            docUser.setPermissionGroup(permissionGroup);
        }
        DocCustomization customization = docCustomizationService.selectOneByDocIdAndUserId(docId, userId);
        if (customization == null) {
            customization = docCustomizationService.settingToFavorite(docId, userId, false);
        }
        docUser.setCustomization(customization);
        return docUser;
    }

    @Override
    @Transactional
    public DocUser getDocUserByShare(PermissionShareDTO share) throws BizException {
        DocUser docUser = new DocUser();
        Long docId = share.getDocId();
        Doc doc = docService.getById(docId);
        docUser.setDoc(doc);
        // judgement share document whether anyone visit
        ShareMode mode = share.getMode();
        Long permissionGroupId = share.getPermissionGroupId();
        DocPermissionGroup permissionGroup = permissionGroupService.getById(permissionGroupId);
        // share mode visit document
        mode.visit(share, docUser);
        if (permissionGroup != null) {
            String permissionJson = permissionGroup.getPermission();
            DocPermission permission = JsonUtils.parse(permissionJson, DocPermission.class);
            docUser.setPermission(permission);
            docUser.setPermissionGroup(permissionGroup);
        }
        if (ShareMode.ANYONE == mode && !AuthUtil.hasAuthentication()) {
            return docUser;
        }
        // set doc user customization
        String userId = docUser.getUserId();
        DocCustomization customization = docCustomizationService.selectOneByDocIdAndUserId(docId, userId);
        if (customization == null) {
            customization = docCustomizationService.settingToFavorite(docId, userId, false);
        }
        docUser.setCustomization(customization);
        return docUser;
    }

    @Override
    public List<DocumentDTO> searchMineDocument(TurboUser currentUser, String pattern) throws BizException {
        DocPageDTO docPageDTO = new DocPageDTO();
        docPageDTO.setCreator(currentUser.getUserId());
        docPageDTO.setCollaborator(currentUser.getUserId());

        List<DocVO> docVOS = docUserMapper.searchUserDocList(docPageDTO);
        List<DocumentDTO> documentList = handleDocumentList(docVOS);

        return documentList.stream()
                .filter(doc -> doc.getTitle().contains(pattern) || doc.getCreateName().contains(pattern))
                .toList();
    }

    @Override
    public IPage<DocumentDTO> selectRecentlyDocument(TurboUser currentUser, DocPageDTO params) throws BizException {
        return selectUserDocument(currentUser, params);
    }

    @Override
    public IPage<DocumentDTO> selectShareToMeDocument(TurboUser currentUser, DocPageDTO params) throws BizException {
        params.setShared(true);
        return selectUserDocument(currentUser, params);
    }

    @Override
    public IPage<DocumentDTO> selectMineFavoriteDocument(TurboUser currentUser, DocPageDTO params) throws BizException {
        params.setFavorite(true);
        return selectUserDocument(currentUser, params);
    }

    @Override
    public IPage<DocumentDTO> selectMineCreateDocument(TurboUser currentUser, DocPageDTO params) throws BizException {
        return selectUserDocument(currentUser, params);
    }

    @Override
    public IPage<DocumentDTO> selectMineFavorDocument(TurboUser currentUser, DocPageDTO params) throws BizException {
        params.setFavor(true);
        return selectUserDocument(currentUser, params);
    }

    @Override
    public List<OnlineDocUser> getOnlineDocUser(TurboUser currentUser, Long docId) throws BizException {
        Doc doc = docService.getById(docId);
        if (doc == null) {
            throw new BizException("not found document");
        }
        return getOnlineUserFromCommand(doc.getKey());
    }

    @Override
    public Boolean kickout(Long docId, List<Long> userIds) throws BizException {
        if (CollectionUtils.isEmpty(userIds)) {
            return true;
        }
        Doc doc = docService.getById(docId);
        if (doc == null) {
            throw new BizException("not found document");
        }
        List<String> users = userIds.stream().map(String::valueOf).toList();
        return dropUsers(doc.getKey(), users);
    }

    @Override
    public Boolean kickoutOthres(TurboUser currentUser, Long docId) throws BizException {
        Doc doc = docService.getById(docId);
        if (doc == null) {
            throw new BizException("not found document");
        }
        String docKey = doc.getKey();
        List<OnlineDocUser> onlineDocUser = getOnlineUserFromCommand(docKey);
        List<String> users = onlineDocUser.stream().map(OnlineDocUser::getUserId).map(String::valueOf).toList();
        Collection<String> otherUsers = CollectionUtils.complement(users, Lists.newArrayList(currentUser.getUserId().toString()));
        return dropUsers(docKey, Lists.newArrayList(otherUsers));
    }

    @Override
    public Boolean kickoutAll(Long docId) throws BizException {
        Doc doc = docService.getById(docId);
        if (doc == null) {
            throw new BizException("not found document");
        }
        String docKey = doc.getKey();
        List<OnlineDocUser> onlineDocUser = getOnlineUserFromCommand(docKey);
        List<String> users = onlineDocUser.stream().map(OnlineDocUser::getUserId).map(String::valueOf).toList();
        return dropUsers(docKey, users);
    }

    /**
     * get online user from command callback
     *
     * @param docKey the doc user
     * @return the {@link OnlineDocUser} user list
     */
    List<OnlineDocUser> getOnlineUserFromCommand(String docKey) {
        CallbackBus.Subscriber subscribe = CallbackBus.subscribe();
        Result result = commandManager.info().execute(docKey, EmptyArgs.identifier);
        ResultCode code = result.getCode();
        if (code == ResultCode.noError) {
            Track track;
            try {
                track = subscribe.get(5L, TimeUnit.SECONDS);
            } catch (Throwable ex) {
                log.error("Failed to get online user from callback event bus, now return empty list", ex);
                return Collections.emptyList();
            }
            if (track == null) {
                return Collections.emptyList();
            }
            List<String> users = track.getUsers();

            if (CollectionUtils.isEmpty(users)) {
                return Collections.emptyList();
            }

            List<Long> userIds = users.stream().map(Long::valueOf).toList();
            List<SysUser> orgUserModels =
                    sysUserService.list(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIds));
            return orgUserModels.stream()
                    .map(user -> {
                        OnlineDocUser onlineDocUser = new OnlineDocUser();
                        onlineDocUser.setDocKey(docKey);
                        onlineDocUser.setUserId(user.getId());
                        onlineDocUser.setUserName(user.getNickname());
                        return onlineDocUser;
                    })
                    .toList();
        } else {
            CallbackBus.remove(subscribe);
        }
        return Collections.emptyList();
    }

    /**
     * drop users from document use by {@link CommandManager}
     *
     * @param docKey the doc key
     * @param users  the user id list
     * @return success if true
     */
    Boolean dropUsers(String docKey, List<String> users) {
        DropArgs dropArgs = DropArgs.builder().users(users).build();
        Result result = commandManager.drop().execute(docKey, dropArgs);
        ResultCode code = result.getCode();
        return code == ResultCode.noError;
    }

    @Override
    public Boolean forceSave(TurboUser currentUser, Long docId) throws BizException {
        Doc doc = docService.getById(docId);
        if (doc == null) {
            throw new BizException("not found document");
        }
        kickoutAll(docId);
        String userId = currentUser.getUserId();
        ForceSaveArgs args = ForceSaveArgs.builder().userdata(userId).build();
        Result result = commandManager.forceSave().execute(doc.getKey(), args);
        return result.getCode() == ResultCode.noError;
    }

    /**
     * select mine document
     *
     * @param currentUser
     * @param params      the {@link DocPageDTO} instance
     * @return the {@link DocumentDTO} list
     */
    @Override
    public IPage<DocumentDTO> selectUserDocument(TurboUser currentUser, DocPageDTO params) throws BizException {
        params.setCreator(currentUser.getUserId());
        params.setCollaborator(currentUser.getUserId());
        IPage<DocVO> docPageList = docUserMapper.selectUserDocList(params);
        List<DocVO> records = docPageList.getRecords();
        List<DocumentDTO> documentList = handleDocumentList(records);
        IPage<DocumentDTO> pageOfDocument = new Page<>();
        pageOfDocument.setTotal(docPageList.getTotal());
        pageOfDocument.setCurrent(docPageList.getCurrent());
        pageOfDocument.setSize(docPageList.getSize());
        pageOfDocument.setRecords(documentList);
        return pageOfDocument;
    }

    /**
     * handle {@link DocVO} data from database.
     * <p>replace user id to user name has creator and collaborator</p>
     *
     * @param records the page of {@link DocVO}
     * @return
     */
    List<DocumentDTO> handleDocumentList(List<DocVO> records) {
        // build user ids
        Set<String> creatorList =
                records.stream().map(DocVO::getCreator).filter(Objects::nonNull).collect(Collectors.toSet());

        Set<String> collaboratorList =
                records.stream().map(DocVO::getCooperator).filter(Objects::nonNull).collect(Collectors.toSet());

        Set<String> userIdList = Sets.newHashSet();
        userIdList.addAll(creatorList);
        userIdList.addAll(collaboratorList);
        Map<String, SysUser> idKeyUser = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(userIdList)) {
            List<SysUser> orgUserModels =
                    sysUserService.list(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIdList));
            idKeyUser = orgUserModels.stream().collect(Collectors.toMap(k -> k.getId().toString(), k -> k));
        }
        Map<String, SysUser> finalIdKeyUser = idKeyUser;
        return records.stream()
                .map(doc -> {
                    DocumentDTO documentDTO = new DocumentDTO();
                    BeanUtils.copyProperties(doc, documentDTO);

                    // set collaborator name
                    String cooperator = documentDTO.getCooperator();
                    Optional.ofNullable(cooperator)
                            .map(finalIdKeyUser::get)
                            .map(SysUser::getNickname)
                            .ifPresent(documentDTO::setCollaboratorName);

                    // set creator name
                    String creator = documentDTO.getCreator();
                    Optional.ofNullable(creator)
                            .map(finalIdKeyUser::get)
                            .map(SysUser::getNickname)
                            .ifPresent(documentDTO::setCreateName);
                    return documentDTO;
                })
                .toList();
    }
}
