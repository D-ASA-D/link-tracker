package backend.academy.linktracker.scrapper.model.orm;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
public class ChatEntity {

    @Id
    private Long id;

    public ChatEntity(Long id) {
        this.id = id;
    }
}
