package com.ecommerce.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.user.entity.Address;
import com.ecommerce.user.mapper.AddressMapper;
import com.ecommerce.user.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public List<Address> getUserAddresses(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId).orderByDesc(Address::getIsDefault).orderByDesc(Address::getCreateTime);
        return addressMapper.selectList(wrapper);
    }

    @Override
    public Address getAddressById(Long addressId) {
        return addressMapper.selectById(addressId);
    }

    @Override
    public Address addAddress(Address address) {
        List<Address> existing = getUserAddresses(address.getUserId());
        if (existing.isEmpty() || Boolean.TRUE.equals(address.getIsDefault())) {
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }
        addressMapper.insert(address);
        return address;
    }

    @Override
    public Address updateAddress(Address address) {
        addressMapper.updateById(address);
        return address;
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        Address addr = addressMapper.selectById(addressId);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        addressMapper.deleteById(addressId);
    }

    @Override
    @Transactional
    public void setDefault(Long addressId, Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId);
        List<Address> userAddresses = addressMapper.selectList(wrapper);
        for (Address a : userAddresses) {
            a.setIsDefault(a.getId().equals(addressId) ? 1 : 0);
            addressMapper.updateById(a);
        }
    }
}
