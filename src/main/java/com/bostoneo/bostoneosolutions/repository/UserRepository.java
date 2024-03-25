package com.bostoneo.bostoneosolutions.repository;

import com.bostoneo.bostoneosolutions.model.User;

import java.util.Collection;

public interface UserRepository<T extends User> {
    /* Basic CRUD Operations */
    T create(T data);
    Collection<T> list (int page, int pageSize); //to be able to page the data, not grabbing all at once from db
    T get(Long id);
    T update(T data);
    Boolean delete(Long id); //boolean to determine if the operation was successful

    /* More Complex Operations */
}
