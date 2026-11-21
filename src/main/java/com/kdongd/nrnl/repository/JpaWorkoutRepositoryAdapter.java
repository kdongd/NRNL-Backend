package com.kdongd.nrnl.repository;

import com.kdongd.nrnl.domain.RunningWorkout;
import com.kdongd.nrnl.domain.Workout;
import com.kdongd.nrnl.domain.WorkoutType;
import com.kdongd.nrnl.dto.WorkoutMonthlyStat;
import com.kdongd.nrnl.dto.WorkoutStatByType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Primary
public class JpaWorkoutRepositoryAdapter implements WorkoutRepository, WorkoutQueryRepository {

    private static final Set<String> SUBCLASS_SORT_PROPERTIES = Set.of("distanceKm");

    private final JpaWorkoutRepository jpaWorkoutRepository;

    public JpaWorkoutRepositoryAdapter(JpaWorkoutRepository jpaWorkoutRepository) {
        this.jpaWorkoutRepository = jpaWorkoutRepository;
    }

    @Override
    public Workout save(Workout workout) {
        return jpaWorkoutRepository.save(workout);
    }

    @Override
    public Optional<Workout> findById(Long id) {
        return jpaWorkoutRepository.findByIdWithDetails(id);
    }

    @Override
    public void delete(Workout workout) {
        jpaWorkoutRepository.delete(workout);
    }

    @Override
    public Page<Workout> search(Long ownerId, WorkoutType type, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<Workout> spec = buildSpecification(ownerId, type, from, to);

        boolean needsCustomSort = pageable.getSort().stream()
                .anyMatch(order -> SUBCLASS_SORT_PROPERTIES.contains(order.getProperty()));

        if (needsCustomSort) {
            spec = spec.and(buildSortSpecification(pageable.getSort()));
            Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            return jpaWorkoutRepository.findAll(spec, unsortedPageable);
        }

        return jpaWorkoutRepository.findAll(spec, pageable);
    }

    private Specification<Workout> buildSortSpecification(Sort sort) {
        return (root, query, cb) -> {
            // count 쿼리(결과 타입이 Long)에는 정렬을 적용하지 않는다.
            // query.orderBy()가 count 쿼리에도 호출되면 SQL이 깨지거나
            // 결과가 왜곡될 수 있어(예: LIMIT과 무관하게 totalElements가 틀어짐).
            if (Long.class.equals(query.getResultType())) {
                return cb.conjunction();
            }

            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();

            for (Sort.Order order : sort) {
                Path<?> path = switch (order.getProperty()) {
                    case "distanceKm" -> cb.treat(root, RunningWorkout.class).get("distanceKm");
                    default -> root.get(order.getProperty());
                };

                if (order.getProperty().equals("distanceKm")) {
                    Expression<Integer> nullsLastKey = cb.<Integer>selectCase()
                            .when(cb.isNull(path), 1)
                            .otherwise(0);
                    orders.add(cb.asc(nullsLastKey));
                }

                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            }

            query.orderBy(orders);
            return cb.conjunction();
        };
    }

    private Specification<Workout> buildSpecification(Long ownerId, WorkoutType type, LocalDateTime from, LocalDateTime to) {
        List<Specification<Workout>> specs = new ArrayList<>();
        specs.add(WorkoutSpecifications.hasOwner(ownerId));
        if (type != null) specs.add(WorkoutSpecifications.hasType(type));
        if (from != null) specs.add(WorkoutSpecifications.fromDate(from));
        if (to != null) specs.add(WorkoutSpecifications.toDate(to));

        return specs.stream()
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    @Override
    public List<WorkoutStatByType> statsByType(Long ownerId, LocalDateTime from, LocalDateTime to) {
        return jpaWorkoutRepository.statsByType(ownerId, from, to);
    }

    @Override
    public List<WorkoutMonthlyStat> statsByMonth(Long ownerId, LocalDateTime from, LocalDateTime to) {
        return jpaWorkoutRepository.statsByMonth(ownerId, from, to);
    }
}
