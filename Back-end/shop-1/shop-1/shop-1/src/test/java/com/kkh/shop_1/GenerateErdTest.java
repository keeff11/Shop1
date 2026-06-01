package com.kkh.shop_1;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@SpringBootTest(properties = {
        // 1. 가상 환경에서 구동될 인메모리 H2 디비 강제 지정
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",

        // 2. 외부 환경변수를 요구하는 기타 설정 dummy 처리
        "jwt.secret=dummySecretKeyWithEnoughLengthForJwtSigning1234567890",
        "spring.data.elasticsearch.repositories.enabled=false",
        "spring.data.redis.repositories.enabled=false"
})
// 3. GitHub 러너에 설치되어 있지 않은 무거운 인프라 자동 설정을 제외하여 구동 속도 최적화 및 에러 방지
@EnableAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        ElasticsearchDataAutoConfiguration.class,
        ElasticsearchRepositoriesAutoConfiguration.class,
        MailSenderAutoConfiguration.class
})
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
            // 주의: 이전에 복사하실 때 주석 태그가 누락되어 복구했습니다.
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