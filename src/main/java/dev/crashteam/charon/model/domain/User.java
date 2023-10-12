package dev.crashteam.charon.model.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "app_user")
public class User {

    @Id
    private String id;

    @Column(name = "balance")
    private Long balance;

    @Column(name = "currency")
    private String currency;

}
