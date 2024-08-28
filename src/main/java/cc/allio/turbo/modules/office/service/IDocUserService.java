package cc.allio.turbo.modules.office.service;

import cc.allio.turbo.common.exception.BizException;
import cc.allio.turbo.modules.auth.provider.TurboUser;
import cc.allio.turbo.modules.office.dto.DocumentDTO;
import cc.allio.turbo.modules.office.dto.OnlineDocUser;
import cc.allio.turbo.modules.office.dto.PermissionShareDTO;
import cc.allio.turbo.modules.office.dto.page.DocPageDTO;
import cc.allio.turbo.modules.office.vo.DocUser;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 与文档用户相关操作
 *
 * @author j.x
 * @date 2024/5/9 18:42
 * @since 0.0.1
 */
public interface IDocUserService {

    /**
     * collect specifies document
     *
     * @param docId the document id
     * @return success if true
     */
    Boolean favoriteOfDocument(TurboUser currentUser, Long docId) throws BizException;

    /**
     * cancel collect specifies document
     *
     * @param currentUser
     * @param docId       the document id
     * @return success if true
     */
    Boolean cancelFavoriteOfDocument(TurboUser currentUser, Long docId) throws BizException;

    /**
     * favor specifies document
     *
     * @param currentUser
     * @param docId       the document id
     * @return success if true
     */
    Boolean favorOfDocument(TurboUser currentUser, Long docId) throws BizException;

    /**
     * cancel favor specifies document
     *
     * @param currentUser
     * @param docId       the document id
     * @return success if true
     */
    Boolean cancelFavorOfDocument(TurboUser currentUser, Long docId) throws BizException;

    /**
     * 从请求中的当前用户获取{@link DocUser}
     *
     * @param currentUser
     * @param docId       the document id
     * @return the {@link DocUser} instance
     */
    DocUser getDocUserByCurrentUser(TurboUser currentUser, Long docId) throws BizException;

    /**
     * from shared document get {@link DocUser} instance
     *
     * @param share the {@link PermissionShareDTO} instance
     * @return the {@link DocUser} instance
     */
    DocUser getDocUserByShare(PermissionShareDTO share) throws BizException;

    /**
     * according document name or creator name
     *
     * @param currentUser
     * @param pattern     the match pattern
     * @return list of {@link DocumentDTO}
     */
    List<DocumentDTO> searchMineDocument(TurboUser currentUser, String pattern) throws BizException;

    /**
     * select user document list
     *
     * @param currentUser
     * @param params      the filter conditional
     * @return the {@link DocumentDTO}
     */
    IPage<DocumentDTO> selectUserDocument(TurboUser currentUser, DocPageDTO params) throws BizException;

    /**
     * select recently document list
     *
     * @param currentUser
     * @param params      the filter conditional
     * @return the {@link DocumentDTO}
     */
    IPage<DocumentDTO> selectRecentlyDocument(TurboUser currentUser, DocPageDTO params) throws BizException;

    /**
     * select share to me document list
     *
     * @param currentUser
     * @param params      the filter conditional
     * @return the {@link DocumentDTO}
     */
    IPage<DocumentDTO> selectShareToMeDocument(TurboUser currentUser, DocPageDTO params) throws BizException;

    /**
     * select mien favorite document list
     *
     * @param currentUser
     * @param params      the filter conditional
     * @return the {@link DocumentDTO}
     */
    IPage<DocumentDTO> selectMineFavoriteDocument(TurboUser currentUser, DocPageDTO params) throws BizException;

    /**
     * select mine create document list
     *
     * @param currentUser
     * @param params      the filter conditional
     * @return the {@link DocumentDTO}
     */
    IPage<DocumentDTO> selectMineCreateDocument(TurboUser currentUser, DocPageDTO params) throws BizException;

    /**
     * select mien favor document list
     *
     * @param currentUser
     * @param params      the filter conditional
     * @return the {@link DocumentDTO}
     */
    IPage<DocumentDTO> selectMineFavorDocument(TurboUser currentUser, DocPageDTO params) throws BizException;

    /**
     * get online doc user
     *
     * @param currentUser
     * @param docId       the doc user
     * @return {@link OnlineDocUser} list
     */
    List<OnlineDocUser> getOnlineDocUser(TurboUser currentUser, Long docId) throws BizException;

    /**
     * kickout specifies document from user ids list
     *
     * @param docId the doc id
     * @param userIds the user id list
     * @return success if true
     */
    Boolean kickout(Long docId, List<Long> userIds) throws BizException;

    /**
     * kickout other users and without creator
     *
     * @param currentUser
     * @param docId       the doc id
     * @return success if true
     */
    Boolean kickoutOthres(TurboUser currentUser, Long docId) throws BizException;

    /**
     * kickout specifies all user
     *
     * @param docId the doc id
     * @return
     */
    Boolean kickoutAll(Long docId) throws BizException;

    /**
     * force save specifies document
     *
     * @param currentUser
     * @param docId       the doc id
     * @return success if true
     */
    Boolean forceSave(TurboUser currentUser, Long docId) throws BizException;
}
