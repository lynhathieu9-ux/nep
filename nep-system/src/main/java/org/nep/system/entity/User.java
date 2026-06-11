package org.nep.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nep.common.base.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nep_user")
public class User extends BaseEntity {
    private String phone;
    private String password;
    private String realName;
    private Integer age;
    private Integer gender;
    private String role;
    private String employeeCode;
    private Long provinceId;
    private Long cityId;
    private String avatar;
    private String email;
    private Integer status;
}
