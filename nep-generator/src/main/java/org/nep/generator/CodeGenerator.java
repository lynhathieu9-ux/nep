package org.nep.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import java.util.Collections;

/**
 * MyBatis-Plus 代码生成器（分布式多模块版）
 *
 * 使用方法：
 *   1. 修改 DB_PASSWORD 为数据库密码
 *   2. 指定要生成的表名（TABLE_NAMES数组）
 *   3. 运行 main 方法
 *
 * 生成内容将输出到 nep-system 模块中：
 *   - Entity     → nep-system/src/main/java/org/nep/system/entity/
 *   - Mapper     → nep-system/src/main/java/org/nep/system/mapper/
 *   - Service    → nep-system/src/main/java/org/nep/system/service/
 *   - Controller → nep-system/src/main/java/org/nep/system/controller/
 *   - Mapper XML → nep-system/src/main/resources/mapper/
 */
public class CodeGenerator {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/nep_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "1234";

    private static final String TABLE_PREFIX = "nep_";
    private static final String[] TABLE_NAMES = {
            "nep_user", "nep_province", "nep_city",
            "nep_supervision_feedback", "nep_aqi_detection", "nep_grid_assignment"
    };

    private static final String SUPER_ENTITY = "org.nep.common.base.BaseEntity";
    private static final String[] SUPER_COLUMNS = {"id", "create_time", "update_time", "deleted"};

    public static void main(String[] args) {
        // 自动定位 nep-system 模块路径
        String userDir = System.getProperty("user.dir");
        String javaDir = userDir + "/../nep-system/src/main/java";
        String xmlDir = userDir + "/../nep-system/src/main/resources/mapper";

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   MyBatis-Plus 代码生成器 v1.0          ║");
        System.out.println("║   东软环保公众监督系统 NEP（分布式版）    ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("Java输出: " + javaDir);
        System.out.println("XML输出:  " + xmlDir);

        FastAutoGenerator.create(DB_URL, DB_USERNAME, DB_PASSWORD)
                .globalConfig(b -> b.author("NEP-Team").outputDir(javaDir).commentDate("yyyy-MM-dd"))
                .packageConfig(b -> b
                        .parent("org.nep.system")
                        .entity("entity").mapper("mapper").service("service")
                        .serviceImpl("service.impl").controller("controller")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, xmlDir)))
                .strategyConfig(b -> b
                        .addTablePrefix(TABLE_PREFIX).addInclude(TABLE_NAMES)
                        .entityBuilder()
                        .superClass(SUPER_ENTITY).addSuperEntityColumns(SUPER_COLUMNS)
                        .enableLombok().enableTableFieldAnnotation().enableFileOverride()
                        .logicDeleteColumnName("deleted")
                        .mapperBuilder()
                        .superClass("com.baomidou.mybatisplus.core.mapper.BaseMapper")
                        .enableBaseResultMap().enableBaseColumnList().enableFileOverride()
                        .serviceBuilder()
                        .superServiceClass("com.baomidou.mybatisplus.extension.service.IService")
                        .superServiceImplClass("com.baomidou.mybatisplus.extension.service.impl.ServiceImpl")
                        .formatServiceFileName("%sService").formatServiceImplFileName("%sServiceImpl")
                        .enableFileOverride()
                        .controllerBuilder().enableRestStyle().enableHyphenStyle().enableFileOverride())
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        System.out.println("✅ 代码生成完毕！请刷新 nep-system 模块查看。");
    }
}
