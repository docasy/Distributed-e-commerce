package com.ecommerce.user.service;

import com.ecommerce.user.entity.Address;
import java.util.List;

public interface AddressService {

    List<Address> getUserAddresses(Long userId);

    Address getAddressById(Long addressId);

    Address addAddress(Address address);

    Address updateAddress(Address address);

    void deleteAddress(Long addressId, Long userId);

    void setDefault(Long addressId, Long userId);
}
