package dev.crashteam.charon.model.domain;

import lombok.Data;

import jakarta.persistence.*;

@Data
@Entity
public class OperationType {

    @Id
    @SequenceGenerator(name = "operationTypeIdSeq", sequenceName = "operation_type_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "operationTypeIdSeq")
    private Long id;

    @Column(name = "type")
    private String type;
}
