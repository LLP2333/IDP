/**
 * 权限模块对外暴露的注解（命名接口）。
 *
 * <p>{@link com.qvqw.idp.permission.annotation.HasPermission} 在 user / role / option 等模块的
 * Controller 上使用，所以需要显式暴露为 NamedInterface。</p>
 */
@org.springframework.modulith.NamedInterface("annotation")
package com.qvqw.idp.permission.annotation;
