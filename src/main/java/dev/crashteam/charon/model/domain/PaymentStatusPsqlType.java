package dev.crashteam.charon.model.domain;

import dev.crashteam.charon.model.RequestPaymentStatus;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.CustomType;

import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class PaymentStatusPsqlType implements UserType<RequestPaymentStatus> {

    @Override
    public int getSqlType() {
        return 0;
    }

    @Override
    public Class<RequestPaymentStatus> returnedClass() {
        return null;
    }

    @Override
    public boolean equals(RequestPaymentStatus x, RequestPaymentStatus y) {
        return false;
    }

    @Override
    public int hashCode(RequestPaymentStatus x) {
        return 0;
    }

    @Override
    public RequestPaymentStatus nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        return null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, RequestPaymentStatus value, int index, SharedSessionContractImplementor session) throws SQLException {

    }

    @Override
    public RequestPaymentStatus deepCopy(RequestPaymentStatus value) {
        return null;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(RequestPaymentStatus value) {
        return null;
    }

    @Override
    public RequestPaymentStatus assemble(Serializable cached, Object owner) {
        return null;
    }
}
