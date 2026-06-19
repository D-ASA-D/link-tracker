package backend.academy.linktracker.scrapper.model.orm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_tags")
@IdClass(SubscriptionTagId.class)
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionTagEntity {

    @Id
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    public SubscriptionTagEntity(Long subscriptionId, Long tagId) {
        this.subscriptionId = subscriptionId;
        this.tagId = tagId;
    }
}
