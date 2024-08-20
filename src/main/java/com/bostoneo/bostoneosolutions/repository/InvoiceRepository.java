package com.bostoneo.bostoneosolutions.repository;

import com.bostoneo.bostoneosolutions.model.Customer;

import com.bostoneo.bostoneosolutions.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface InvoiceRepository extends PagingAndSortingRepository<Invoice, Long>, ListCrudRepository<Invoice, Long> {

}
