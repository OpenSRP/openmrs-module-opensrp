package org.ei.drishti.service;

import java.util.List;

import org.ei.drishti.domain.ServiceProvided;
import org.ei.drishti.repository.AllServicesProvided;

public class ServiceProvidedService {
    private AllServicesProvided allServiceProvided;

    public ServiceProvidedService(AllServicesProvided allServicesProvided) {
        this.allServiceProvided = allServicesProvided;
    }

    public List<ServiceProvided> findByEntityIdAndServiceNames(String entityId, String... names) {
        return allServiceProvided.findByEntityIdAndServiceNames(entityId, names);
    }

    public void add(ServiceProvided serviceProvided) {
        allServiceProvided.add(serviceProvided);
    }
}
