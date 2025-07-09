package com.mindbridge.ai.agent.orchestrator.models.entity;

import com.mindbridge.ai.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "mood_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoodEntry extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "mood_score")
    private Integer moodScore; // 1-10 scale

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "mood_tags", columnDefinition = "text[]")
    private List<String> moodTags;

    // TODO word limit
    @Column(columnDefinition = "TEXT")
    private String notes;

}
