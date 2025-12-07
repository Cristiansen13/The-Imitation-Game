package imitationgame.chatservice.repository;

import imitationgame.chatservice.model.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageLogRepository extends JpaRepository<MessageLog, String> {
}

