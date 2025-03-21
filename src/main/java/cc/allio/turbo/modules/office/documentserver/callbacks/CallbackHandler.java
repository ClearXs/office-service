package cc.allio.turbo.modules.office.documentserver.callbacks;

import cc.allio.turbo.modules.office.documentserver.vo.Action;
import cc.allio.turbo.modules.office.documentserver.vo.Track;
import cc.allio.turbo.modules.office.entity.DocChanges;
import cc.allio.turbo.modules.office.service.IDocChangesService;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class CallbackHandler {

    private final Map<Integer, Callback> callbackHandlers;
    private final IDocChangesService docChangesService;

    public CallbackHandler(IDocChangesService docChangesService) {
        this.docChangesService = docChangesService;
        this.callbackHandlers = Maps.newHashMap();
    }

    public void register(final int code, final Callback callback) {  // register a callback handler
        callbackHandlers.put(code, callback);
    }

    /**
     * handle and dispatch document callback. and return <a href="https://api.onlyoffice.com/editors/callback#changeshistory">handle status </a>
     *
     * @param docId    the doc id
     * @param fileId   the file id
     * @param fileName the file name
     * @param body     the callback body {@link Track}
     * @return handle status
     */
    public int handle(Long docId, Long fileId, String fileName, Track body) {
        Integer status = body.getStatus();
        String docKey = body.getKey();
        if (log.isDebugEnabled()) {
            log.debug("handle doc [docId={} docKey={} filename={}]  callback trigger action [{}], the url={} the changesurl={}", docId, docKey, fileName, status, body.getUrl(), body.getChangesurl());
        }
        if (status == null) {
            return 0;
        }

        // trigger doc changes
        List<Action> actions = body.getActions();
        List<DocChanges> docChangesList =
                actions.stream()
                        .map(action -> {
                            DocChanges docChanges = new DocChanges();
                            docChanges.setUserId(action.getUserid());
                            docChanges.setAction(action.getType().name());
                            docChanges.setDocId(docId);
                            return docChanges;
                        })
                        .toList();
        docChangesService.saveBatch(docChangesList);

        // get specifies status
        Callback callback = callbackHandlers.get(status);
        if (callback == null) {
            log.warn("Callback status {} is not supported yet", body.getStatus());
            return 0;
        }

        // publish to even bus
        CallbackBus.publish(body);

        return callback.handle(docId, fileId, fileName, body);
    }

}
