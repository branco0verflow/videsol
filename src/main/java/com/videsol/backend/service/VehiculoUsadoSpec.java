package com.videsol.backend.service;

import com.videsol.backend.entity.VehiculoUsado;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class VehiculoUsadoSpec {

    public static Specification<VehiculoUsado> activo() {
        return (root, query, cb) ->
                cb.isTrue(root.get("activo"));
    }

    public static Specification<VehiculoUsado> marca(String marca) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("marca")), marca.toLowerCase());
    }

    public static Specification<VehiculoUsado> tipo(String tipo) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("tipo")), tipo.toLowerCase());
    }

    public static Specification<VehiculoUsado> combustible(String combustible) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("combustible")), combustible.toLowerCase());
    }

    public static Specification<VehiculoUsado> transmision(String transmision) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("transmision")),
                        "%" + transmision.toLowerCase() + "%");
    }

    public static Specification<VehiculoUsado> precioMax(BigDecimal precioMax) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("precioRef"), precioMax);
    }

    public static Specification<VehiculoUsado> kmMax(Integer kmMax) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("km"), kmMax);
    }

    public static Specification<VehiculoUsado> anioMin(Integer anioMin) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("anio"), anioMin);
    }

    // Ordenar por últimos ingresos y/o por precio < o >
    public static Specification<VehiculoUsado> ordenarPor(String sort, String order) {
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