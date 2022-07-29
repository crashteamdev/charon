package dev.crashteam.charon.model;


import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Data
@MappedSuperclass
public class TimestampEntity {

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created", updatable = false)
    private Date createDate;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated")
    private Date modifyDate;

    @Override
    public String toString() {
        return "TimestampEntity{" +
                "createDate=" + createDate +
                ", modifyDate=" + modifyDate +
                '}';
    }
}
