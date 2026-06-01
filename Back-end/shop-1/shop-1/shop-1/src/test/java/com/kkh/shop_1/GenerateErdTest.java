package com.kkh.shop_1;

import com.kkh.shop_1.common.config.QuerydslConfig; // 💡 Querydsl 설정 임포트 추가
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import; // 💡 Import 어노테이션 추가

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(QuerydslConfig.class) // 💡 핵심: @DataJpaTest에서 누락된 QueryDSL 빈 설정을 수동으로 주입합니다.
public class GenerateErdTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void generateMermaidErdAndInjectToReadme() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("erDiagram\n");

        Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();

        // 1. 엔티티 및 컬럼 파싱
        for (EntityType<?> entity : entities) {
            String tableName = entity.getJavaType().getSimpleName();
            sb.append("    ").append(tableName).append(" {\n");

            for (Attribute<?, ?> attribute : entity.getAttributes()) {
                if (!attribute.isAssociation()) {
                    String type = attribute.getJavaType().getSimpleName();
                    String name = attribute.getName();
                    sb.append("        ").append(type).append(" ").append(name).append("\n");
                }
            }
            sb.append("    }\n\n");
        }

        // 2. 연관관계 파싱
        for (EntityType<?> entity : entities) {
            String sourceTable = entity.getJavaType().getSimpleName();
            for (Attribute<?, ?> attribute : entity.getAttributes()) {
                if (attribute.isAssociation()) {
                    String targetTable;
                    if (attribute instanceof PluralAttribute) {
                        targetTable = ((PluralAttribute<?, ?, ?>) attribute).getElementType().getJavaType().getSimpleName();
                        sb.append("    ").append(sourceTable).append(" ||--{ ").append(targetTable).append(" : \"has\"\n");
                    } else {
                        targetTable = attribute.getJavaType().getSimpleName();
                        sb.append("    ").append(sourceTable).append(" }|--|| ").append(targetTable).append(" : \"references\"\n");
                    }
                }
            }
        }
        sb.append("```\n");

        updateReadmeWithErd(sb.toString());
    }

    private void updateReadmeWithErd(String mermaidCode) throws IOException {
        Path readmePath = Paths.get("../../../../README.md");
        if (!Files.exists(readmePath)) {
            System.out.println("README.md 파일을 찾을 수 없습니다: " + readmePath.toAbsolutePath());
            return;
        }

        List<String> lines = Files.readAllLines(readmePath, StandardCharsets.UTF_8);
        StringBuilder newContent = new StringBuilder();
        boolean inErdBlock = false;

        for (String line : lines) {
            if (line.contains("")) {
                newContent.append(line).append("\n");
                newContent.append(mermaidCode);
                inErdBlock = true;
                continue;
            }
            if (line.contains("")) {
                inErdBlock = false;
            }
            if (!inErdBlock) {
                newContent.append(line).append("\n");
            }
        }

        Files.write(readmePath, newContent.toString().getBytes(StandardCharsets.UTF_8));
    }
}