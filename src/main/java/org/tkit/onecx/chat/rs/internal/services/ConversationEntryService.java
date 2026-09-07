package org.tkit.onecx.chat.rs.internal.services;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.tkit.onecx.chat.domain.daos.ConversationEntryDAO;
import org.tkit.onecx.chat.domain.models.Chat;
import org.tkit.onecx.chat.domain.models.ConversationEntry;

import gen.org.tkit.onecx.chat.rs.internal.model.CreateOrUpdateConversationEntryDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public class ConversationEntryService {

    @Inject
    ConversationEntryDAO dao;

    @Transactional
    public ConversationEntryOutcome createOrUpdate(Chat chat, CreateOrUpdateConversationEntryDTO dto) {
        var existing = dao.findByChatIdAndIdempotencyKey(chat.getId(), dto.getIdempotencyKey());

        if (existing.isEmpty()) {
            var entry = new ConversationEntry();
            entry.setChat(chat);
            entry.setIdempotencyKey(dto.getIdempotencyKey());
            entry.setEntryType(ConversationEntry.EntryType.valueOf(dto.getEntryType().name()));
            entry.setText(dto.getText());
            entry.setUserId(dto.getUserId());
            entry.setStatus(dto.getStatus() != null ? ConversationEntry.Status.valueOf(dto.getStatus().name())
                    : ConversationEntry.Status.IN_PROGRESS);
            entry.setSequenceNumber(dao.findMaxSequenceNumberForChat(chat.getId()) + 1);

            dao.create(entry);
            return new ConversationEntryOutcome(entry, true);
        }

        var current = existing.get();

        if (current.getStatus() != ConversationEntry.Status.IN_PROGRESS) {
            throw new ConversationEntryConflictException(
                    "Entry is already finalized with terminal status " + current.getStatus());
        }

        var requestedText = dto.getText() == null ? "" : dto.getText();
        var existingText = current.getText() == null ? "" : current.getText();
        var requestedStatus = dto.getStatus() != null ? ConversationEntry.Status.valueOf(dto.getStatus().name())
                : ConversationEntry.Status.IN_PROGRESS;

        if (requestedText.equals(existingText) && requestedStatus == current.getStatus()) {
            return new ConversationEntryOutcome(current, false);
        }

        if (requestedText.startsWith(existingText)) {
            current.setText(dto.getText());
            if (dto.getStatus() != null) {
                current.setStatus(ConversationEntry.Status.valueOf(dto.getStatus().name()));
            }
            dao.update(current);
            return new ConversationEntryOutcome(current, false);
        }

        throw new ConversationEntryConflictException(
                "Incompatible retry: requested text does not extend the stored cumulative text");
    }

    public List<ConversationEntry> replay(Chat chat) {
        return dao.findVisibleTerminalEntriesForChat(chat.getId());
    }

    public static class ConversationEntryConflictException extends RuntimeException {
        public ConversationEntryConflictException(String message) {
            super(message);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ConversationEntryOutcome {
        private final ConversationEntry entry;
        private final boolean created;
    }
}
