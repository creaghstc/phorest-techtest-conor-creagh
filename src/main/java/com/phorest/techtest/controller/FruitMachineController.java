package com.phorest.techtest.controller;

import com.phorest.techtest.model.MachineState;
import com.phorest.techtest.service.FruitMachineService;
import com.phorest.techtest.web.ConfigureMachineRequestDto;
import com.phorest.techtest.web.MachineStateResponseDto;
import com.phorest.techtest.web.PlayResponseDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fruit-machine")
public class FruitMachineController {

    private final FruitMachineService fruitMachineService;

    public FruitMachineController(FruitMachineService fruitMachineService) {
        this.fruitMachineService = fruitMachineService;
    }

    @PostMapping("/play")
    public PlayResponseDto play() {
        return PlayResponseDto.from(this.fruitMachineService.play());
    }

    @GetMapping
    public MachineStateResponseDto state() {
        return MachineStateResponseDto.from(this.fruitMachineService.state());
    }

    @PutMapping
    public MachineStateResponseDto configure(@Valid @RequestBody ConfigureMachineRequestDto request) {
        MachineState state = this.fruitMachineService.configure(
                request.initialBalance(),
                request.costPerPlay(),
                request.slotCount(),
                request.colourCount(),
                request.smallPrizeRunLength());
        return MachineStateResponseDto.from(state);
    }
}
