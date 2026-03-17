package com.rivercom.claw_machine_backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "prize_redemptions", schema = "app")
public class PrizeRedemptions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prize_id", nullable = false)
    private Prize prize;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_transaction_id", nullable = false, unique = true)
    private PointTransaction pointTransaction;

    @Column(name = "points_spent", nullable = false)
    private  Integer pointsSpent;

    @Column(name = "redeemed_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime redeemedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private MachineCampaign campaign;
}
