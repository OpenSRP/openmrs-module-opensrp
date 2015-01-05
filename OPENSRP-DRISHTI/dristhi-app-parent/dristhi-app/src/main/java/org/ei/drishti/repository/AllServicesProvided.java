package org.ei.drishti.repository;

import java.util.List;

import org.ei.drishti.domain.ServiceProvided;

public class AllServicesProvided {
    private ServiceProvidedRepository repository;

    public AllServicesProvided(ServiceProvidedRepository repository) {
        this.repository = repository;
    }

    public List<ServiceProvided> findByEntityIdAndServiceNames(String entityId, String... names) {
        return repository.findByEntityIdAndServiceNames(entityId, names);
    }

    public void add(ServiceProvided serviceProvided) {
        repository.add(serviceProvided);
    }
}
