package com.fypgrading.evaluationservice.service.mapper;

import java.util.List;

/**
 * Entity-DTO mapper interface, providing essential mapper methods
 * @param <U> Entity class
 * @param <T> DTO class
 */
public interface EntityMapper<U, T> {

    T toDTO(U entity);

    List<T> toDTOList(List<U> entityList);

    U toEntity(T dto);

    List<U> toEntityList(List<T> dtoList);

}
