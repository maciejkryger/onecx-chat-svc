package org.tkit.onecx.chat.rs.internal.services;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.chat.domain.daos.ConversationEntryDAO;
import org.tkit.onecx.chat.domain.models.Chat;
import org.tkit.onecx.chat.domain.models.ConversationEntry;
import org.tkit.onecx.chat.rs.internal.mappers.ChatMapper;
import org.tkit.quarkus.jpa.exceptions.ConstraintException;

import gen.org.tkit.onecx.chat.rs.internal.model.CreateOrUpdateConversationEntryDTO;
import gen.org.tkit.onecx.chat.rs.internal.model.EntryStatusDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ConversationEntryServiceTest {

    @Inject
    ConversationEntryService service;

    @InjectMock
    ConversationEntryDAO dao;

    @InjectMock
    ChatMapper chatMapper;

    @Test
    void createOrUpdateShouldLoadExistingEntryAfterConstraintViolation() {

        Chat chat = new Chat();
        chat.setId("chat-1");

        CreateOrUpdateConversationEntryDTO dto = new CreateOrUpdateConversationEntryDTO();

        dto.setIdempotencyKey("idem-1");
        dto.setText("Hello");
        dto.setStatus(EntryStatusDTO.IN_PROGRESS);

        ConversationEntry existing = new ConversationEntry();
        existing.setIdempotencyKey("idem-1");
        existing.setText("Hello");
        existing.setStatus(ConversationEntry.EntryStatus.IN_PROGRESS);

        when(chatMapper.mapConversationStatus(dto.getStatus()))
                .thenReturn(ConversationEntry.EntryStatus.IN_PROGRESS);

        when(dao.findByChatAndIdempotencyKey(chat, "idem-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));

        when(dao.findMaxSequenceByChat(chat))
                .thenReturn(0L);

        ConstraintException exception = org.mockito.Mockito.mock(ConstraintException.class);

        when(dao.create(any(ConversationEntry.class)))
                .thenThrow(exception);

        ConversationEntry result = org.junit.jupiter.api.Assertions
                .assertDoesNotThrow(() -> service.createOrUpdate(chat, dto));

        assertSame(existing, result);

        verify(dao, times(2))
                .findByChatAndIdempotencyKey(chat, "idem-1");
    }
}
