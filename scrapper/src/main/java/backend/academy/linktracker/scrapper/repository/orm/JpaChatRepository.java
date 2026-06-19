package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.orm.ChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaChatRepository extends JpaRepository<ChatEntity, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO chats (id)
        VALUES (:chatId)
        ON CONFLICT (id) DO NOTHING
        """, nativeQuery = true)
    int insertIfNotExists(@Param("chatId") Long chatId);

    @Modifying
    @Query(value = """
        DELETE FROM chats
        WHERE id = :chatId
        """, nativeQuery = true)
    int deleteIfExists(@Param("chatId") Long chatId);
}
