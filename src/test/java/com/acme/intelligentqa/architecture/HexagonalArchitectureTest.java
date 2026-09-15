package com.acme.intelligentqa.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.acme.intelligentqa.IntelligentQaApplication;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 验证 HexagonalArchitecture 的业务行为与边界。
 */
@AnalyzeClasses(packagesOf = IntelligentQaApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    /**
     * 领域层不得依赖基础设施框架的架构规则。
     */
    @ArchTest
    static final ArchRule DOMAIN_IS_FRAMEWORK_INDEPENDENT = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..", "org.apache.ibatis..", "org.mybatis..",
                    "javax..", "jakarta..", "..adapter..", "..application..");

    /**
     * 应用层不得依赖适配器或持久化框架的架构规则。
     */
    @ArchTest
    static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_ADAPTERS = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "org.apache.ibatis..", "org.mybatis..");

    /**
     * 按基础设施职责命名适配器的架构规则。
     */
    @ArchTest
    static final ArchRule ADAPTERS_ARE_NAMED_BY_ROLE = classes()
            .that().resideInAPackage("..adapter.out..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Adapter")
            .orShould().haveSimpleNameEndingWith("Repository")
            .orShould().haveSimpleNameEndingWith("Mapper")
            .orShould().haveSimpleNameEndingWith("Record");

    /**
     * 领域端口必须声明为接口的架构规则。
     */
    @ArchTest
    static final ArchRule PORTS_ARE_INTERFACES = classes()
            .that().resideInAPackage("..domain.port..")
            .and().areTopLevelClasses()
            .should().beInterfaces();
}
