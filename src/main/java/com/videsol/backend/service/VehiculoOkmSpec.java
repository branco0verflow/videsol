package com.videsol.backend.service;

import com.videsol.backend.entity.VehiculoOkm;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class VehiculoOkmSpec {

    public static Specification<VehiculoOkm> activo() {
        return (root, query, cb) ->
                cb.isTrue(root.get("activo"));
    }

    public static Specification<VehiculoOkm> tipo(String tipo) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("tipo")), tipo.toLowerCase());
    }

    public static Specification<VehiculoOkm> combustible(String combustible) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("combustible")), combustible.toLowerCase());
    }

    public static Specification<VehiculoOkm> transmision(String transmision) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("transmision")), "%" + transmision.toLowerCase() + "%");
    }

    public static Specification<VehiculoOkm> marca(String marca) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("marcaRef")), marca.toLowerCase());
    }

    public static Specification<VehiculoOkm> precioMax(BigDecimal precioMax) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("precioRef"), precioMax);
    }

    // Ordenar por últimos ingresos y/o por precio < o >
    public static Specification<VehiculoOkm> ordenarPor(String sort, String order) {
        return (root, query, cb) -> {
            boolean asc = !"desc".equalsIgnoreCase(order);
            if ("reciente".equalsIgnoreCase(sort)) {
                query.orderBy(asc ? cb.asc(root.get("id")) : cb.desc(root.get("id")));
            } else if ("precio".equalsIgnoreCase(sort)) {
                query.orderBy(asc ? cb.asc(root.get("precioRef")) : cb.desc(root.get("precioRef")));
            }
            return cb.conjunction();
        };
    }
}