package cc.allio.turbo.modules.system.controller;

import cc.allio.turbo.common.web.TurboTreeCrudController;
import cc.allio.turbo.modules.system.domain.SysMenuTree;
import cc.allio.turbo.modules.system.entity.SysMenu;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sys/menu")
@AllArgsConstructor
@Tag(name = "菜单")
public class SysMenuController extends TurboTreeCrudController<SysMenu, SysMenuTree> {

}
