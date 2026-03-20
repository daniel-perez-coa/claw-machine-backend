package com.rivercom.claw_machine_backend.mapper;

import com.rivercom.claw_machine_backend.domain.entity.Prize;
import com.rivercom.claw_machine_backend.dto.PrizeDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PrizeMapper {

    public PrizeDTO toResponsePrize(Prize prize) {
        if  (prize == null) {
            return null;
        }
        return new PrizeDTO(
                prize.getId(),
                prize.getCategory(),
                prize.getName(),
                prize.getDescription(),
                prize.getPointsCost(),
                prize.getCost()
        );
    }

    public List<PrizeDTO> toResponsePrizeList(List<Prize> prizes) {
        if (prizes == null) {
            return null;
        }
        return prizes.stream()
                .map(this::toResponsePrize)
                .toList();
    }
}
