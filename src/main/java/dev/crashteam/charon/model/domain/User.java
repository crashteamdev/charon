package dev.crashteam.charon.model.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
