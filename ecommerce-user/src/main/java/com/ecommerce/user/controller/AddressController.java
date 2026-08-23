package com.ecommerce.user.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.common.utils.JwtUtil;
import com.ecommerce.user.entity.Address;
import com.ecommerce.user.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping
    public Result<List<Address>> list(@RequestHeader("Authorization") String token) {
        Long userId = JwtUtil.getUserId(token);
        return Result.success(addressService.getUserAddresses(userId));
    }

    @GetMapping("/{id}")
    public Result<Address> getById(@PathVariable Long id) {
        return Result.success(addressService.getAddressById(id));
    }

    @PostMapping
    public Result<Address> add(@RequestHeader("Authorization") String token, @RequestBody Address address) {
        address.setUserId(JwtUtil.getUserId(token));
        return Result.success(addressService.addAddress(address));
    }

    @PutMapping("/{id}")
    public Result<Address> update(@RequestHeader("Authorization") String token, @PathVariable Long id, @RequestBody Address address) {
        address.setId(id);
        address.setUserId(JwtUtil.getUserId(token));
        return Result.success(addressService.updateAddress(address));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        addressService.deleteAddress(id, JwtUtil.getUserId(token));
        return Result.success(null);
    }

    @PostMapping("/{id}/default")
    public Result<Void> setDefault(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        addressService.setDefault(id, JwtUtil.getUserId(token));
        return Result.success(null);
    }
}
