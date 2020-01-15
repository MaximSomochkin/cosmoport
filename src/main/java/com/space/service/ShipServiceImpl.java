package com.space.service;

import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import com.space.exceptions.BadRequestException;
import com.space.exceptions.ShipNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
@Component
public class ShipServiceImpl implements ShipService {
    @Autowired
    private  ShipRepository shipRepository;
    @Override
    public Page<Ship> getAllShips(Specification<Ship> specification, Pageable storedByName) {
        return shipRepository.findAll(specification,storedByName);
    }

    @Override
    public List<Ship> getAllShips(Specification<Ship> specification) {

        return shipRepository.findAll(specification);
    }

    @Override
    public Ship createShip(Ship ship) {
        if (ship.getName()==null
                    ||ship.getShipType()==null
                    ||ship.getPlanet()==null
                    ||ship.getCrewSize()==null
                    ||ship.getProdDate()==null
                    ||ship.getSpeed()==null)
            throw new BadRequestException("One of ship parameter is null");

        checkShipParams(ship);

        if(ship.getUsed()==null)
            ship.setUsed(false);

        Double raiting = calculateRating(ship);
        ship.setRating(raiting);
        return shipRepository.saveAndFlush(ship);
    }



    @Override
    public Ship getShip(Long id) {
        if(!shipRepository.existsById(id))
            throw new ShipNotFoundException("Ship not found");

        return shipRepository.findById(id).get();
    }

    @Override
    public Ship editShip(Long id, Ship ship) {
        checkShipParams(ship);

        if(!shipRepository.existsById(id))
            throw new ShipNotFoundException("Ship not found by id"+ id);

        Ship editedShip = shipRepository.findById(id).get();

        if (ship.getName() != null)
            editedShip.setName(ship.getName());

        if (ship.getPlanet() != null)
            editedShip.setPlanet(ship.getPlanet());

        if (ship.getShipType() != null)
            editedShip.setShipType(ship.getShipType());

        if (ship.getProdDate() != null)
            editedShip.setProdDate(ship.getProdDate());

        if (ship.getSpeed() != null)
            editedShip.setSpeed(ship.getSpeed());

        if (ship.getUsed() != null)
            editedShip.setUsed(ship.getUsed());

        if (ship.getCrewSize() != null)
            editedShip.setCrewSize(ship.getCrewSize());

        Double rating = calculateRating(editedShip);
        editedShip.setRating(rating);

        return shipRepository.saveAndFlush(editedShip);
    }

    @Override
    public void deleteById(Long id) {
        if(shipRepository.existsById(id))
            shipRepository.deleteById(id);

        else throw new ShipNotFoundException("Ship not found");
    }

    @Override
    public Long checkAndParseId(String id) {
        if (id == null || id.equals("") || id.equals("0"))
            throw new BadRequestException("Incorrect ID");

        try {
            Long longId = Long.parseLong(id);
            return longId;
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID is not number", e);
        }

    }

    @Override
    public Specification<Ship> filterByPlanet(String planet) {
        return (root, query, criteriaBuilder) ->
                planet==null?null:criteriaBuilder.like(root.get("planet"),"%"+planet+"%");
    }

    @Override
    public Specification<Ship> filterByName(String name) {
        return (root, query, criteriaBuilder) ->
                name==null?null:criteriaBuilder.like(root.get("name"),"%"+name+"%");
    }

    @Override
    public Specification<Ship> filterByShipType(ShipType shipType) {
        return (root, query, criteriaBuilder) ->
               shipType==null?null:criteriaBuilder.equal(root.get("shipType"),shipType);

    }

    @Override
    public Specification<Ship> filterByDate(Long after, Long before) {
        return (root, query, cb) -> {
            if (after == null && before == null)
                return null;
            if (after == null) {
                Date before1 = new Date(before);
                return cb.lessThanOrEqualTo(root.get("prodDate"), before1);
            }
            if (before == null) {
                Date after1 = new Date(after);
                return cb.greaterThanOrEqualTo(root.get("prodDate"), after1);
            }
            Date before1 = new Date(before);
            Date after1 = new Date(after);

            return cb.between(root.get("prodDate"), after1, before1);
        };
    }

    @Override
    public Specification<Ship> filterByUsage(Boolean isUsed) {
        return ((root, query, criteriaBuilder) ->{
            if(isUsed==null)
                return null;
            if(isUsed)
                return criteriaBuilder.isTrue(root.get("isUsed"));
            else return criteriaBuilder.isFalse(root.get("isUsed"));
        });
    }

    @Override
    public Specification<Ship> filterBySpeed(Double min, Double max) {
        return ((root, query, criteriaBuilder) -> {
            if (min==null&&max==null)
                return null;
            if(min==null)
                return criteriaBuilder.lessThanOrEqualTo(root.get("speed"),max);
            if (max==null)
                return criteriaBuilder.greaterThanOrEqualTo(root.get("speed"),min);

             return criteriaBuilder.between(root.get("speed"),min,max);
        });
    }

    @Override
    public Specification<Ship> filterByCrewSize(Integer min, Integer max) {
        return (root, query, criteriaBuilder) -> {
            if (min==null&&max==null)
                return null;
            if (min==null)
                return criteriaBuilder.lessThanOrEqualTo(root.get("crewSize"),max);
            if (max==null)
                return criteriaBuilder.greaterThanOrEqualTo(root.get("crewSize"),min);

            return criteriaBuilder.between(root.get("crewSize"),min,max);
        };
    }

    @Override
    public Specification<Ship> filterByRating(Double min, Double max) {
        return (root, query, criteriaBuilder) -> {
            if(min==null&&max==null)
                return null;
            if(min==null)
             return    criteriaBuilder.lessThanOrEqualTo(root.get("rating"),max);
            if (max==null)
                return criteriaBuilder.greaterThanOrEqualTo(root.get("rating"),min);

            return criteriaBuilder.between(root.get("rating"),min,max);
        };
    }

    private void checkShipParams(Ship ship) {
        if (ship.getName() != null && (ship.getName().length() < 1 || ship.getName().length() > 50))
            throw new BadRequestException("Incorrect Ship.name");

        if (ship.getPlanet() != null && (ship.getPlanet().length() < 1 || ship.getPlanet().length() > 50))
            throw new BadRequestException("Incorrect Ship.planet");

        if (ship.getCrewSize() != null && (ship.getCrewSize() < 1 || ship.getCrewSize() > 9999))
            throw new BadRequestException("Incorrect Ship.crewSize");

        if (ship.getSpeed() != null && (ship.getSpeed() < 0.01D || ship.getSpeed() > 0.99D))
            throw new BadRequestException("Incorrect Ship.speed");

        if (ship.getProdDate() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(ship.getProdDate());
            if (cal.get(Calendar.YEAR) < 2800 || cal.get(Calendar.YEAR) > 3019)
                throw new BadRequestException("Incorrect Ship.date");
        }

    }

    private Double calculateRating(Ship ship) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(ship.getProdDate());
        int year = cal.get(Calendar.YEAR);

        //calculate rating
        BigDecimal raiting = new BigDecimal((80 * ship.getSpeed() * (ship.getUsed() ? 0.5 : 1)) / (3019 - year + 1));
        //round rating to 2 decimal places
        raiting = raiting.setScale(2, RoundingMode.HALF_UP);
        return raiting.doubleValue();


    }

}
