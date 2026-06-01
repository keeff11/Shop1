package com.kkh.shop_1;

import com.kkh.shop_1.common.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

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
@Import(QuerydslConfig.class)
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
                    // 💡 해결 1: Mermaid 파서를 고장내는 배열 기호([]) 및 특수기호 제거
                    String type = attribute.getJavaType().getSimpleName().replace("[]", "");
                    String name = attribute.getName();
                    sb.append("        ").append(type).append(" ").append(name).append("\n");
                }
            }
            sb.append("    }\n\n");
        }

        // 2. 연관관계 파싱 (단방향 최적화)
        for (EntityType<?> entity : entities) {
            String sourceTable = entity.getJavaType().getSimpleName();
            for (Attribute<?, ?> attribute : entity.getAttributes()) {
                if (attribute.isAssociation()) {
                    // 💡 해결 2: 양방향 선이 2개씩 그려져 렌더링이 뻗는 현상 방지
                    // DB의 실제 외래키(FK) 방향인 단일 참조(ManyToOne, OneToOne)일 때만 선을 1개 그립니다.
                    if (!(attribute instanceof PluralAttribute)) {
                        String targetTable = attribute.getJavaType().getSimpleName();
                        sb.append("    ").append(sourceTable).append(" }o--|| ").append(targetTable).append(" : references\n");
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