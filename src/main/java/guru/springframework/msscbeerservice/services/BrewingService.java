package guru.springframework.msscbeerservice.services;

import java.util.List;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import guru.sfg.common.events.BrewBeerEvent;
import guru.springframework.msscbeerservice.config.JmsConfig;
import guru.springframework.msscbeerservice.domain.Beer;
import guru.springframework.msscbeerservice.repositories.BeerRepository;
import guru.springframework.msscbeerservice.services.inventory.BeerInventoryService;
import guru.springframework.msscbeerservice.web.mappers.BeerMapper;
import guru.springframework.msscbeerservice.web.model.BeerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrewingService {
	private final BeerRepository beerRepository;
	private final BeerInventoryService beerInventoryService;
	private final JmsTemplate jmsTemplate;
	private final BeerMapper beerMapper;
	
	@Scheduled(fixedRate = 5000)
	public void checkForLowInventory() {
		List<Beer> beers = beerRepository.findAll();
		
		beers.forEach(beer -> {
			Integer invQOH = beerInventoryService.getOnhandInventory(beer.getId());
			
			log.debug("Min onhand is: " + beer.getMinOnHand());
			log.debug("Inventory is: " + invQOH);
			
			if (beer.getMinOnHand() >= invQOH) {
				BeerDto beerDto = beerMapper.beerToBeerDto(beer);
				jmsTemplate.convertAndSend(JmsConfig.BREWING_REQUEST_QUEUE, new BrewBeerEvent(beerDto));
				log.debug("Sent to queue "+JmsConfig.BREWING_REQUEST_QUEUE+": "+beerDto);
			}
			
		});
	}

}
