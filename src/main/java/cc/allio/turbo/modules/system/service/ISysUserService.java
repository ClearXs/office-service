package cc.allio.turbo.modules.system.service;

import cc.allio.turbo.common.db.mybatis.service.ITurboCrudService;
import cc.allio.turbo.common.exception.BizException;
import cc.allio.turbo.modules.system.domain.SysUserVO;
import cc.allio.turbo.modules.system.dto.BindingOrgDTO;
import cc.allio.turbo.modules.system.dto.BindingPostDTO;
import cc.allio.turbo.modules.system.dto.BindingRoleDTO;
import cc.allio.turbo.modules.system.entity.SysUser;

public interface ISysUserService extends ITurboCrudService<SysUser> {

    /**
     * 根据用户名获取用户
     *
     * @param username username
     * @return sysuser or null
     */
    SysUserVO findByUsername(String username) throws BizException;

    /**
     * obtains user details
     * <ul>
     *     <li>post</li>
     *     <li>role</li>
     *     <li>organization</li>
     *     <li>...</li>
     * </ul>
     *
     * @param sysUser the sys user
     * @return a {@link SysUserVO} instance
     */
    SysUserVO findUserDetails(SysUser sysUser);

    /**
     * find third user by third user id
     *
     * @param thirdUserId the third user id
     * @return the {@link SysUserVO} instance or null
     */
    SysUserVO findThirdUserDetails(String thirdUserId);

    /**
     * 用户绑定角色
     *
     * @param bindingRole bindingRole
     * @return
     */
    Boolean bindingRole(BindingRoleDTO bindingRole);

    /**
     * 更改密码
     *
     * @param userId      用户id
     * @param newPassword 新密码
     * @return
     */
    Boolean changePassword(Long userId, String newPassword) throws BizException;

    /**
     * 绑定组织
     *
     * @param bindingOrg bindingOrg
     */
    Boolean bindingOrg(BindingOrgDTO bindingOrg);

    /**
     * 绑定岗位
     *
     * @param bindingPost bindingPost
     */
    Boolean bindingPost(BindingPostDTO bindingPost);

}
