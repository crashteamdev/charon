package dev.crashteam.charon.repository.specification;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.payment.PaymentsQuery;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

@Data
@RequiredArgsConstructor
public class PaymentSpecification implements Specification<Payment> {

    private final PaymentsQuery paymentsQuery;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Predicate toPredicate(Root<Payment> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        var predicates = new ArrayList<Predicate>();

        if (StringUtils.hasText(paymentsQuery.getUserId())) {
            var queryString = paymentsQuery.getUserId().toLowerCase(Locale.ROOT);
            predicates.add(
                    builder.equal(builder.lower(root.get("userId")), queryString)
            );
        }

        if (paymentsQuery.hasDateFrom() && StringUtils.hasText(paymentsQuery.getDateFrom().getValue())) {
            LocalDateTime dateTime = LocalDate.parse(paymentsQuery.getDateFrom().getValue(), formatter).atStartOfDay();
            predicates.add(
                    builder.greaterThanOrEqualTo(root.get("created"), dateTime)
            );
        }

        if (paymentsQuery.hasDateTo() && StringUtils.hasText(paymentsQuery.getDateTo().getValue())) {
            LocalDateTime dateTime = LocalDate.parse(paymentsQuery.getDateTo().getValue(), formatter).atStartOfDay();
            predicates.add(
                    builder.lessThanOrEqualTo(root.get("created"), dateTime)
            );
        }

        predicates.add(builder.equal(root.get("status"), RequestPaymentStatus.SUCCESS));
        return builder.and(predicates.toArray(new Predicate[0]));
    }
}
