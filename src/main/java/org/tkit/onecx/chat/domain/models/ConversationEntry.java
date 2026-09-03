package org.tkit.onecx.chat.domain.models;

import jakarta.persistence.*;

import org.hibernate.annotations.TenantId;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "CONVERSATION_ENTRY", uniqueConstraints = {
        @UniqueConstraint(name = "UK_CONV_ENTRY_CHAT_SEQ", columnNames = { "CHAT_ID", "SEQUENCE_NO" }),
        @UniqueConstraint(name = "UK_CONV_ENTRY_IDEMP", columnNames = { "CHAT_ID", "IDEMPOTENCY_KEY" })
})
public class ConversationEntry extends TraceableEntity {

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHAT_ID", nullable = false)
    private Chat chat;

    @Column(name = "SEQUENCE_NO", nullable = false)
    private Long sequence;

    @Column(name = "IDEMPOTENCY_KEY", nullable = false)
    private String idempotencyKey;

    @Column(name = "TEXT", length = 10000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private EntryType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private EntryStatus status;

    public enum EntryType {
        HUMAN,
        ASSISTANT
    }

    public enum EntryStatus {
        IN_PROGRESS,
        COMPLETED,
        INTERRUPTED
    }
}
