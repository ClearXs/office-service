package cc.allio.turbo.modules.office.service.impl;

import cc.allio.turbo.common.db.mybatis.service.impl.TurboCrudServiceImpl;
import cc.allio.turbo.common.exception.BizException;
import cc.allio.turbo.common.util.AuthUtil;
import cc.allio.turbo.common.util.RedisUtil;
import cc.allio.turbo.common.util.SecureUtil;
import cc.allio.turbo.extension.oss.*;
import cc.allio.turbo.extension.oss.request.OssPutRequest;
import cc.allio.turbo.modules.office.constant.DocType;
import cc.allio.turbo.modules.office.constant.ShareExpired;
import cc.allio.turbo.modules.office.constant.ShareMode;
import cc.allio.turbo.modules.office.constant.WebhookType;
import cc.allio.turbo.modules.office.documentserver.command.CommandManager;
import cc.allio.turbo.modules.office.documentserver.command.Result;
import cc.allio.turbo.modules.office.documentserver.command.ResultCode;
import cc.allio.turbo.modules.office.documentserver.command.requestinfo.MetaArgs;
import cc.allio.turbo.modules.office.documentserver.managers.document.DocumentManager;
import cc.allio.turbo.modules.office.documentserver.managers.jwt.JwtManager;
import cc.allio.turbo.modules.office.documentserver.models.filemodel.User;
import cc.allio.turbo.modules.office.documentserver.storage.FileStorageMutator;
import cc.allio.turbo.modules.office.documentserver.util.DocDescriptor;
import cc.allio.turbo.modules.office.documentserver.util.file.FileUtility;
import cc.allio.turbo.modules.office.documentserver.util.service.ServiceConverter;
import cc.allio.turbo.modules.office.documentserver.vo.*;
import cc.allio.turbo.modules.office.dto.DocumentCreateDTO;
import cc.allio.turbo.modules.office.dto.PermissionShareDTO;
import cc.allio.turbo.modules.office.dto.ShareDTO;
import cc.allio.turbo.modules.office.entity.*;
import cc.allio.turbo.modules.office.mapper.DocMapper;
import cc.allio.turbo.modules.office.service.*;
import cc.allio.turbo.modules.office.vo.DocPermission;
import cc.allio.turbo.modules.system.entity.SysAttachment;
import cc.allio.turbo.modules.system.entity.SysUser;
import cc.allio.turbo.modules.system.service.ISysAttachmentService;
import cc.allio.turbo.modules.system.service.ISysUserService;
import cc.allio.uno.core.util.*;
import cc.allio.uno.core.util.id.IdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DocServiceImpl extends TurboCrudServiceImpl<DocMapper, Doc> implements IDocService {

    private final IDocPermissionGroupService permissionGroupService;
    private final ISysAttachmentService sysAttachmentService;
    private final IDocCooperatorService docCooperatorService;
    private final IDocHistoryService docHistoryService;
    private final IDocHistoryUserService docHistoryUserService;
    private final ISysUserService sysUserService;
    private final IDocWebhookService docWebhookService;

    private final ServiceConverter serviceConverter;
    private final JwtManager jwtManager;
    private final FileUtility fileUtility;
    private final FileStorageMutator fileStorageMutator;
    private final DocumentManager documentManager;
    private final CommandManager commandManager;

    private final Map<DocType, String> templateFileMapping = Maps.newHashMap();

    public DocServiceImpl(IDocPermissionGroupService permissionGroupService,
                          ISysAttachmentService sysAttachmentService,
                          ISysUserService sysUserService,
                          IDocHistoryService docHistoryService,
                          IDocHistoryUserService docHistoryUserService,
                          IDocCooperatorService docCooperatorService,
                          IDocWebhookService docWebhookService,
                          ServiceConverter serviceConverter,
                          JwtManager jwtManager,
                          CommandManager commandManager,
                          FileUtility fileUtility,
                          DocumentManager documentManager,
                          FileStorageMutator fileStorageMutator) {
        this.permissionGroupService = permissionGroupService;
        this.serviceConverter = serviceConverter;
        this.sysAttachmentService = sysAttachmentService;
        this.docHistoryService = docHistoryService;
        this.docHistoryUserService = docHistoryUserService;
        this.docCooperatorService = docCooperatorService;
        this.docWebhookService = docWebhookService;
        this.sysUserService = sysUserService;
        this.jwtManager = jwtManager;
        this.fileUtility = fileUtility;
        this.fileStorageMutator = fileStorageMutator;
        this.documentManager = documentManager;
        this.commandManager = commandManager;
    }

    @Override
    public boolean save(Doc doc) {
        Long docId = doc.getId();
        if (docId == null) {
            docId = IdGenerator.defaultGenerator().getNextId();
            doc.setId(docId);
        }
        // set type
        String type = doc.getType();
        if (StringUtils.isEmpty(type)) {
            String documentTYpe = DocDescriptor.obtainTypeFromFile(doc);
            doc.setType(documentTYpe);
        }
        // generate new version
        String docKey = serviceConverter.generateRevisionId(String.valueOf(docId));
        // set new doc
        doc.setKey(docKey);
        // set new version
        Integer version = DocDescriptor.trimToVersion(docKey);
        doc.setDocVersion(version);
        // set creator
        String userId = AuthUtil.getUserId();
        doc.setCreator(userId);
        boolean insert = super.save(doc);
        if (insert) {
            permissionGroupService.settingDocPermissionGroup(doc.getId());
            // create current doc version 1 history
            DocHistory docHistory = new DocHistory();
            docHistory.setDocId(doc.getId());
            docHistory.setDocKey(doc.getKey());
            docHistory.setDocTitle(doc.getTitle());
            docHistory.setDocType(doc.getType());
            docHistory.setDocVersion(1);
            docHistory.setMaintainUserId(userId);
            String username = AuthUtil.getUsername();
            docHistory.setMaintainUsername(username);
            docHistory.setFile(doc.getFile());
            docHistoryService.save(docHistory);
        }
        return insert;
    }

    @Override
    public Doc createDocumentFromTemplate(DocumentCreateDTO documentCreate) throws BizException {
        // load blank template
        String templateFilepath =
                templateFileMapping.computeIfAbsent(
                        documentCreate.getType(),
                        type -> {
                            String ext = type.getValue();
                            return "/template/blank." + ext;
                        });

        InputStream blankTemplateStream = this.getClass().getResourceAsStream(templateFilepath);
        if (blankTemplateStream == null) {
            throw new BizException("not found blank template");
        }
        // create attachment
        List<SysAttachment> attachments = Lists.newArrayList();
        long filesize = 0L;
        try {
            filesize = blankTemplateStream.available();
        } catch (IOException ex) {
            log.error("Failed to get filesize", ex);
        }
        String title = documentCreate.getTitle();
        String type = documentCreate.getType().getValue();
        String fullname = title + StringPool.DOT + type;
        Doc doc = new Doc();
        Long docId = IdGenerator.defaultGenerator().getNextId();
        doc.setId(docId);
        String filepath = fileUtility.getFilePath(docId, fullname);
        // put attachment to minio
        OssPutRequest request = OssPutRequest.builder().path(Path.from(filepath)).inputStream(blankTemplateStream).build();
        Path upload = OssExecutorFactory.getCurrent().upload(request);
        SysAttachment attachment = createDocumentAttachment(upload.getFullPath(), fullname, upload.compose(), type, filesize);
        boolean insert = sysAttachmentService.save(attachment);
        if (insert) {
            attachments.add(attachment);
        }
        doc.setTitle(title);
        doc.setType(type);
        String attachmentJson = JsonUtils.toJson(attachments);
        doc.setFile(attachmentJson);
        return onNewlyDocument(doc);
    }

    @Override
    public Doc upload(Long id, String filename, String downloadUrl) throws IOException, BizException {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .get()
                .build();

        Response response =
                new OkHttpClient.Builder()
                        .build()
                        .newCall(request)
                        .execute();
        InputStream io = response.body().byteStream();
        return save(id, filename, io);
    }

    @Override
    public Doc saves(MultipartFile file) throws IOException, BizException {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isEmpty(originalFilename)) {
            throw new BizException("file name is empty");
        }
        return save(IdGenerator.defaultGenerator().getNextId(), file.getOriginalFilename(), file.getInputStream());
    }

    /**
     * save document
     *
     * @param docId
     * @param originalFilename
     * @param io
     * @return
     * @throws BizException
     */
    Doc save(Long docId, String originalFilename, InputStream io) throws BizException {
        String fileName = originalFilename.substring(0, originalFilename.lastIndexOf(StringPool.DOT));
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(StringPool.DOT) + 1);
        Doc doc = new Doc();
        doc.setId(docId);
        doc.setTitle(fileName);
        doc.setType(fileExtension);
        String filepath = fileUtility.getFilePath(docId, originalFilename);
        OssPutRequest request = OssPutRequest.builder().path(Path.from(filepath)).inputStream(io).build();
        Path path = OssExecutorFactory.getCurrent().upload(request);

        SysAttachment sysAttachment = new SysAttachment();
        sysAttachment.setFilename(originalFilename);
        String filetype = originalFilename.substring(originalFilename.lastIndexOf(cc.allio.uno.core.StringPool.ORIGIN_DOT));
        sysAttachment.setFiletype(filetype);
        // 文件大小
        int filesize = 0;
        try {
            filesize = io.available();
        } catch (IOException e) {
            // ignore
        }
        sysAttachment.setFilesize((long) filesize);
        sysAttachment.setFilepath(path.compose());
        sysAttachmentService.save(sysAttachment);
        String docJsons = JsonUtils.toJson(Lists.newArrayList(sysAttachment));
        doc.setFile(docJsons);
        return onNewlyDocument(doc);
    }

    @Override
    public void forceSave(Long docId, String filename, Track track) {
        String literalFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        onProcessForceSave(docId, literalFilename, track);
    }

    /**
     * trigger new document.it will be save to database and insert version 1 history and put in minio
     *
     * @return the
     * @throws BizException failed to save
     */
    Doc onNewlyDocument(Doc waitForDoc) throws BizException {
        boolean insert = save(waitForDoc);
        if (insert) {
            DocDescriptor docDescriptor = new DocDescriptor(waitForDoc);
            Long docId = docDescriptor.getDocId();
            String fullname = docDescriptor.getFullname();
            String docKey = docDescriptor.getDocKey();
            Track track = new Track();
            track.setKey(docKey);
            History history = new History();
            User user = new User();
            user.setId(waitForDoc.getCreator().toString());
            history.setUser(user);
            track.setHistory(history);
            // trigger to process history
            onProcessHistorySave(docDescriptor, docId, fullname, track);
            return waitForDoc;
        } else {
            throw new BizException("failed to save document!");
        }
    }

    @Override
    public void newVersion(Long docId, String filename, Track track) {
        String literalFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        DocDescriptor docDescriptor = onProcessSave(docId, literalFilename, track);
        if (docDescriptor != null) {
            onProcessHistorySave(docDescriptor, docId, literalFilename, track);
        }
    }

    @Override
    public Boolean rename(Long docId, Rename rename) throws BizException {
        Doc doc = getById(docId);
        if (doc == null) {
            throw new BizException("not found document");
        }
        String newfilename = rename.getNewfilename();
        String fullname = newfilename;
        String dockey = doc.getKey();
        String origExt = doc.getType();
        String curExt = fullname;
        if (fullname.contains(".")) {
            curExt = fileUtility.getFileExtension(fullname);
        }
        if (origExt.compareTo(curExt) != 0) {
            fullname += StringPool.DOT + origExt;
        }

        // rename from onlyoffice
        MetaArgs args = MetaArgs.builder().meta(new MetaArgs.Meta(fullname)).build();
        Result result = commandManager.meta().execute(dockey, args);
        if (ResultCode.noError != result.getCode()) {
            return false;
        }

        DocDescriptor docDescriptor = new DocDescriptor(doc);

        // rename minio file
        SysAttachment docAttachment = docDescriptor.obtainAttachment();
        String fileJson = StringPool.EMPTY;
        if (docAttachment != null) {
            String src = docAttachment.getFilepath();
            String dest = src.replace(docAttachment.getFilename(), fullname);
            OssExecutorFactory.getCurrent().copyObject(src, dest);
            docAttachment.setFilepath(dest);
            List<SysAttachment> attachments = Lists.newArrayList(docAttachment);
            fileJson = JsonUtils.toJson(attachments);
        }

        // update to database doc name
        return update(
                new Doc(),
                Wrappers.<Doc>lambdaUpdate()
                        .set(Doc::getTitle, newfilename)
                        .set(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(fileJson), Doc::getFile, fileJson)
                        .eq(Doc::getId, docId));
    }

    @Override
    public Boolean restore(Long docId, Integer version) {
        Doc doc = getOne(Wrappers.<Doc>lambdaQuery().eq(Doc::getId, docId));
        if (doc == null) {
            return false;
        }
        DocHistory docHistory =
                docHistoryService.getOne(Wrappers.<DocHistory>lambdaQuery().eq(DocHistory::getDocId, docId).eq(DocHistory::getDocVersion, version));
        if (docHistory == null) {
            return false;
        }
        String docKey = docHistory.getDocKey();
        Track track = new Track();
        track.setKey(docKey);
        // set history
        History history = new History();
        history.setKey(docKey);
        history.setVersion(version);
        history.setCreated(DateUtil.formatNow());

        String userId = AuthUtil.getUserId();
        String nickname = AuthUtil.getNickname();
        User user = new User();
        user.setId(userId);
        user.setName(nickname);
        history.setUser(user);
        track.setHistory(history);

        DocDescriptor docDescriptor = new DocDescriptor(docHistory);
        SysAttachment documentAttachment = docDescriptor.obtainAttachment();
        if (documentAttachment != null) {
            String downloadUrl = documentManager.getDownloadUrl(documentAttachment.getFilepath());
            track.setUrl(downloadUrl);
        }
        track.setFiletype(docHistory.getDocType());
        try {
            String fullname = DocDescriptor.getFullname(doc);
            newVersion(docId, fullname, track);
        } catch (Throwable ex) {
            log.error("Failed to restore document version.", ex);
            return false;
        }
        return true;
    }

    @Override
    public HistoryList getHistoryList(Long docId) throws BizException {
        Doc currentDoc = getById(docId);
        if (currentDoc == null) {
            throw new BizException("not found document!");
        }
        HistoryList history = new HistoryList();
        history.setCurrentVersion(currentDoc.getDocVersion());

        // find history by doc id and user id
        LambdaQueryWrapper<DocHistory> historyQueryWrapper = Wrappers.<DocHistory>lambdaQuery().eq(DocHistory::getDocId, docId).orderByAsc(DocHistory::getDocVersion);
        List<DocHistory> docHistories = docHistoryService.list(historyQueryWrapper);

        List<HistoryList.HistoryInfo> historyInfos =
                docHistories.stream()
                        .map(docHistory -> {
                            DocDescriptor docDescriptor = new DocDescriptor(docHistory);
                            HistoryList.HistoryInfo historyInfo = new HistoryList.HistoryInfo();
                            historyInfo.setKey(docDescriptor.getDocKey());
                            historyInfo.setServerVersion(docHistory.getServerVersion());
                            historyInfo.setVersion(docDescriptor.getVersion());
                            historyInfo.setCreated(docDescriptor.getCreateTime());
                            // set history user
                            HistoryList.HistoryUser historyUser = new HistoryList.HistoryUser();
                            String userId = docHistory.getMaintainUserId();
                            String username = docHistory.getMaintainUsername();
                            historyUser.setId(userId);
                            historyUser.setName(username);
                            historyInfo.setUser(historyUser);
                            // set user and change user
                            List<DocHistoryUser> docHistoryUserList =
                                    docHistoryUserService.list(Wrappers.<DocHistoryUser>lambdaQuery().eq(DocHistoryUser::getHistoryId, docHistory.getId()));
                            if (CollectionUtils.isNotEmpty(docHistoryUserList)) {
                                // set change user list
                                List<ChangesHistory> changesHistoryList =
                                        docHistoryUserList.stream()
                                                .map(user -> {
                                                    ChangesHistory changesHistory = new ChangesHistory();
                                                    changesHistory.setCreated(DateUtil.formatDate(user.getChangedTime()));
                                                    ChangesUser changesUser = new ChangesUser();
                                                    changesUser.setId(String.valueOf(user.getUserId()));
                                                    changesUser.setName(user.getUsername());
                                                    changesHistory.setUser(changesUser);
                                                    return changesHistory;
                                                })
                                                .toList();
                                historyInfo.setChanges(changesHistoryList);
                            }
                            return historyInfo;
                        })
                        .toList();

        history.setHistory(historyInfos);

        return history;
    }

    @Override
    public HistoryData getHistoryData(Long docId, Integer version) throws BizException {

        LambdaQueryWrapper<DocHistory> docHistoryWrapper =
                Wrappers.<DocHistory>lambdaQuery()
                        .eq(DocHistory::getDocId, docId)
                        .eq(DocHistory::getDocVersion, version)
                        .last("limit 1");

        DocHistory docHistory = docHistoryService.getOne(docHistoryWrapper);

        if (docHistory == null) {
            throw new BizException("not found the doc version");
        }

        HistoryData historyData = new HistoryData();
        DocDescriptor docDescriptor = new DocDescriptor(docHistory);
        historyData.setVersion(docDescriptor.getVersion());
        historyData.setKey(docDescriptor.getDocKey());

        // set to doc download url
        SysAttachment docAttachment = docDescriptor.obtainAttachment();
        if (docAttachment != null) {
            String attachmentUrl = documentManager.getDownloadUrl(docAttachment.getFilepath());
            historyData.setUrl(attachmentUrl);
            historyData.setFileType(docAttachment.getFiletype());
        }
        // set to changedata download url
        SysAttachment changeDataAttachment = docDescriptor.obtainChangeData();
        if (changeDataAttachment != null) {
            String changeDataDownloadUrl = documentManager.getDownloadUrl(changeDataAttachment.getFilepath());
            historyData.setChangesUrl(changeDataDownloadUrl);
        }

        // get previous attachment
        if (version > 1) {
            LambdaQueryWrapper<DocHistory> previousQueryWrapper = Wrappers.<DocHistory>lambdaQuery()
                    .eq(DocHistory::getDocId, docId)
                    .eq(DocHistory::getDocVersion, version - 1)
                    .last("limit 1");
            DocHistory previousHistory = docHistoryService.getOne(previousQueryWrapper);

            // set previous attachment
            if (previousHistory != null) {
                DocDescriptor previousDocDescriptor = new DocDescriptor(previousHistory);
                HistoryData.Previous previous = new HistoryData.Previous();
                previous.setKey(previousDocDescriptor.getDocKey());
                SysAttachment previousDocDescriptorDocAttachment = previousDocDescriptor.obtainAttachment();
                if (previousDocDescriptorDocAttachment != null) {
                    String previousAttachmentDownloadUrl = documentManager.getDownloadUrl(previousDocDescriptorDocAttachment.getFilepath());
                    previous.setUrl(previousAttachmentDownloadUrl);
                }
                historyData.setPrevious(previous);
            }
        }

        Map<String, Object> claims = JsonUtils.toMap(JsonUtils.toJson(historyData));
        String token = jwtManager.createToken(claims);
        historyData.setToken(token);
        return historyData;
    }

    @Override
    @Transactional
    public Boolean remove(List<Long> docIdList) {
        // remove document
        // remove all collaborator
        return super.removeBatchByIds(docIdList) &&
                docHistoryService.remove(Wrappers.<DocHistory>lambdaQuery().in(DocHistory::getDocId, docIdList)) &&
                docCooperatorService.remove(Wrappers.<DocCooperator>lambdaQuery().in(DocCooperator::getDocId, docIdList));
    }

    @Override
    @Transactional
    public String share(ShareDTO share) throws BizException {
        Long docId = share.getDocId();
        // valid document exiting.
        long count = count(Wrappers.<Doc>lambdaQuery().eq(Doc::getId, docId));
        if (count <= 0) {
            throw new BizException("not fount document");
        }
        String secureKey = SecureUtil.getRandomSecureKey(10);
        DocPermission permission = share.getPermission();
        // create the new permission group
        DocPermissionGroup permissionGroup = new DocPermissionGroup();
        permissionGroup.setDocId(docId);
        permissionGroup.setGroupCode(secureKey);
        permissionGroup.setGroupName(secureKey);
        permissionGroup.setPermission(JsonUtils.toJson(permission));
        permissionGroupService.save(permissionGroup);

        ShareMode mode = share.getMode();
        if (ShareMode.SPECIFIC == mode) {
            List<String> cooperator = share.getCooperator();
            // insert document cooperator
            List<DocCooperator> docCooperators =
                    cooperator.stream()
                            .map(cooperatorId -> {
                                DocCooperator docCooperator = new DocCooperator();
                                docCooperator.setCooperator(cooperatorId);
                                docCooperator.setPermissionGroupId(permissionGroup.getId());
                                docCooperator.setDocId(docId);
                                return docCooperator;
                            })
                            .toList();
            docCooperatorService.saveBatch(docCooperators);
        }
        PermissionShareDTO permissionShare = PermissionShareDTO.from(share);

        permissionShare.setPermissionGroupId(permissionGroup.getId());

        // set share permission to cache
        SecureUtil.SecureCipher cipher = SecureUtil.getSystemSecureCipher();
        ShareExpired expired = permissionShare.getExpired();
        RedisUtil.setEx(secureKey, JsonUtils.toJson(permissionShare), expired.getTime(), TimeUnit.MILLISECONDS);
        return cipher.encrypt(secureKey);
    }

    /**
     * handle force save.
     * <p>it will be download from onlyoffice latest document, but not be create new version</p>
     *
     * @param docId    the  dock id
     * @param filename the filename
     * @param body     the callback {@link Track}
     */
    public DocDescriptor onProcessSave(Long docId, String filename, Track body) {
        String downloadUrl = body.getUrl();
        // download file from onlyoffice server
        byte[] downloadFile = fileStorageMutator.getDownloadFile(downloadUrl);
        int filesize = downloadFile.length;
        Doc latestDoc = getById(docId);
        // caver new document saves to oss and cover old document.
        ByteArrayInputStream is = new ByteArrayInputStream(downloadFile);
        String filepath = fileUtility.getFilePath(docId, filename);
        OssPutRequest request =
                OssPutRequest.builder()
                        .path(Path.from(filepath))
                        .inputStream(is)
                        .build();
        Path path = OssExecutorFactory.getCurrent().upload(request);

        // file info saves to db
        SysAttachment docAttachment = createDocumentAttachment(path.getFullPath(), filename, path.compose(), body.getFiletype(), (long) filesize);
        boolean insert = sysAttachmentService.save(docAttachment);
        if (insert) {
            // reset doc file info
            String fileJson = JsonUtils.toJson(Lists.newArrayList(docAttachment));
            String newVersionDocKey = serviceConverter.generateRevisionId(docId.toString());
            Integer version = DocDescriptor.trimToVersion(newVersionDocKey);
            // update
            LambdaUpdateWrapper<Doc> updateWrapper =
                    Wrappers.<Doc>lambdaUpdate()
                            .eq(Doc::getId, docId)
                            .set(Doc::getFile, fileJson)
                            .set(Doc::getKey, newVersionDocKey)
                            .set(Doc::getDocVersion, version);
            boolean update = update(new Doc(), updateWrapper);
            if (update) {
                latestDoc.setFile(fileJson);
                latestDoc.setDocVersion(version);
                latestDoc.setKey(newVersionDocKey);
            }

            docWebhookService.trigger(WebhookType.EDIT, docId, docAttachment);
            // return latest document version
            return new DocDescriptor(latestDoc);
        }
        return null;
    }

    /**
     * handle force save.
     * <p>it will be download from onlyoffice latest document, but not be create new version</p>
     *
     * @param docId    the  dock id
     * @param filename the filename
     * @param body     the callback {@link Track}
     */
    public void onProcessForceSave(Long docId, String filename, Track body) {
        String downloadUrl = body.getUrl();
        // download file from onlyoffice server
        byte[] downloadFile = fileStorageMutator.getDownloadFile(downloadUrl);
        int filesize = downloadFile.length;
        // caver new document saves to oss and cover old document.
        ByteArrayInputStream is = new ByteArrayInputStream(downloadFile);
        String filepath = fileUtility.getFilePath(docId, filename);
        OssPutRequest request =
                OssPutRequest.builder()
                        .path(Path.from(filepath))
                        .inputStream(is)
                        .build();
        Path path = OssExecutorFactory.getCurrent().upload(request);
        SysAttachment attachment = createDocumentAttachment(path.getFullPath(), filename, path.compose(), body.getFiletype(), (long) filesize);
        boolean insert = sysAttachmentService.save(attachment);
        if (insert) {
            // reset doc file info
            String fileJson = JsonUtils.toJson(Lists.newArrayList(attachment));
            // update
            LambdaUpdateWrapper<Doc> updateWrapper = Wrappers.<Doc>lambdaUpdate().eq(Doc::getId, docId).set(Doc::getFile, fileJson);
            update(new Doc(), updateWrapper);
        }

        docWebhookService.trigger(WebhookType.EDIT, docId, attachment);
    }

    /**
     * handle history doc.
     * <ol>
     *     <li>get current doc file</li>
     *     <li>put to oss and changedata</li>
     *     <li>saves to db</li>
     * </ol>
     *
     * @param latestDocument the old doc
     * @param docId          the doc id
     * @param filename       the file name like as test.docx
     * @param track          the {@link Track} instance
     */
    void onProcessHistorySave(DocDescriptor latestDocument, Long docId, String filename, Track track) {
        // get history attachments
        List<SysAttachment> historyAttachments = getHistoryAttachments(latestDocument, docId, filename, track);
        History history = track.getHistory();
        List<Long> changeUserIds = Lists.newArrayList();
        List<ChangesHistory> changes = Lists.newArrayList();
        if (history != null) {
            // get changes
            changes = history.getChanges();
            if (CollectionUtils.isNotEmpty(changes)) {
                changeUserIds = changes.stream().map(ChangesHistory::getUser).map(ChangesUser::getId).map(Long::parseLong).toList();
            }
        }

        // get change user from system user
        List<SysUser> sysUsers = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(changeUserIds)) {
            sysUsers = sysUserService.list(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, changeUserIds));
        }

        String historyFile = JsonUtils.toJson(historyAttachments);
        DocHistory docHistory = new DocHistory();
        docHistory.setDocId(docId);
        docHistory.setDocTitle(latestDocument.getTitle());
        docHistory.setDocType(latestDocument.getType());
        docHistory.setDocKey(track.getKey());
        docHistory.setFile(historyFile);
        docHistory.setDocVersion(latestDocument.getVersion());
        docHistory.setChangeUrl(track.getChangesurl());
        if (history != null) {
            String changesString = JsonUtils.toJson(history);
            docHistory.setChanges(changesString);
            docHistory.setServerVersion(history.getServerVersion());
        }
        // get history user or find the last user as for maintain user
        List<SysUser> finalOrgUserModels = sysUsers;
        User maintainUser =
                Optional.ofNullable(history)
                        .map(History::getUser)
                        .orElseGet(
                                () -> {
                                    if (CollectionUtils.isNotEmpty(finalOrgUserModels)) {
                                        SysUser latestUser = finalOrgUserModels.getLast();
                                        if (latestUser != null) {
                                            User user = new User();
                                            user.setId(String.valueOf(latestUser.getId()));
                                            user.setName(latestUser.getNickname());
                                            return user;
                                        }
                                    }
                                    return null;
                                });

        if (maintainUser != null) {
            docHistory.setMaintainUserId(maintainUser.getId());
            docHistory.setMaintainUsername(maintainUser.getName());
        }

        // save to db
        docHistoryService.save(docHistory);

        List<DocHistoryUser> docHistoryUserList = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(changes)) {
            docHistoryUserList =
                    changes.stream()
                            .map(change -> {
                                ChangesUser user = change.getUser();
                                String id = user.getId();
                                String name = user.getName();
                                DocHistoryUser docHistoryUser = new DocHistoryUser();
                                // set user info
                                if (StringUtils.isNotBlank(id)) {
                                    docHistoryUser.setUserId(id);
                                    docHistoryUser.setUsername(name);
                                }
                                // set change time
                                String created = change.getCreated();
                                Date changedTime = DateUtil.parse(created);
                                docHistoryUser.setChangedTime(changedTime);
                                docHistoryUser.setHistoryId(docHistory.getId());
                                return docHistoryUser;
                            })
                            .toList();
        }

        if (CollectionUtils.isNotEmpty(docHistoryUserList)) {
            docHistoryUserService.saveBatch(docHistoryUserList);
        }
    }

    /**
     * get history version document attachment.
     * <p>the first add old document attachment, then from changeurl download changedata to minio and database. </p>
     *
     * @param latestDocument the {@link DocDescriptor}
     * @param docId          the document id
     * @param filename       the filename
     * @param track          the {@link Track}
     * @return the {@link SysAttachment} list
     */
    List<SysAttachment> getHistoryAttachments(DocDescriptor latestDocument, Long docId, String filename, Track track) {
        List<SysAttachment> historyAttachments = Lists.newArrayList();
        if (latestDocument == null) {
            return historyAttachments;
        }
        // minio direction like as ${docVersion}/changes..
        String latestDocDir = String.format("%s/", latestDocument.getVersion());

        // handle previous attachment
        SysAttachment previousDocumentAttachment = latestDocument.obtainAttachment();
        if (previousDocumentAttachment != null) {
            // copy source document version to destination document
            // current destination has be ${docVersion}/filename.ext
            String src = previousDocumentAttachment.getFilepath();
            String dest = src.substring(0, src.lastIndexOf(StringPool.SLASH)) + StringPool.SLASH + latestDocDir + filename;
            Path path = OssExecutorFactory.getCurrent().copyObject(src, dest);
            String filetype = previousDocumentAttachment.getFiletype();
            Long filesize = previousDocumentAttachment.getFilesize();
            SysAttachment historyDoc = createDocumentAttachment(path.getFullPath(), filename, path.compose(), filetype, filesize);
            boolean insert = sysAttachmentService.save(historyDoc);
            if (insert) {
                historyAttachments.add(historyDoc);
            }
        }

        String changesurl = track.getChangesurl();
        byte[] changeDataFile = new byte[0];
        try {
            changeDataFile = fileStorageMutator.getDownloadFile(changesurl);
        } catch (Throwable ex) {
            log.error("doc [docId={}] try to download changedata failure, the changeurl is {}", docId, changesurl);
        }
        String docFilePathPrefix = latestDocument.getDocFilePathPrefix();
        SysAttachment changeDataAttachment = null;
        if (changeDataFile != null
                && changeDataFile.length > 0
                && StringUtils.isNotBlank(docFilePathPrefix)) {
            // get download url split '/' lastname
            String changeDataFilename = "changes.zip";
            // put change data
            String changeDataFilepath = fileUtility.getFilePath(docId, latestDocDir + changeDataFilename);
            ByteArrayInputStream is = new ByteArrayInputStream(changeDataFile);
            OssPutRequest request = OssPutRequest.builder().path(Path.from(changeDataFilepath)).inputStream(is).build();
            Path path = OssExecutorFactory.getCurrent().upload(request);

            // create new file attachment entity
            changeDataAttachment =
                    createDocumentAttachment(path.getFullPath(),
                            changeDataFilename, path.compose(),
                            "zip",
                            (long) changeDataFile.length);
            sysAttachmentService.save(changeDataAttachment);
        }
        // add old changes attachment to attachment list
        if (changeDataAttachment != null) {
            historyAttachments.add(changeDataAttachment);
        }

        return historyAttachments;
    }

    /**
     * create document {@link SysAttachment} attachment instance
     *
     * @param filename the filename
     * @param filepath like as /path/name.docx
     * @param filetype the file type
     * @param filesize the file size
     * @return {@link SysAttachment} instance
     */
    SysAttachment createDocumentAttachment(
            String fullPath, String filename, String filepath, String filetype, Long filesize) {

        SysAttachment attachment = new SysAttachment();
        attachment.setFullPath(fullPath);
        attachment.setFilename(filename);
        attachment.setFilepath(filepath);
        attachment.setFiletype(filetype);
        attachment.setFilesize(filesize);
        OssExecutor executor = OssExecutorFactory.getCurrent();
        Provider provider = executor.getProvider();
        attachment.setProvider(provider);
        return attachment;
    }
}
