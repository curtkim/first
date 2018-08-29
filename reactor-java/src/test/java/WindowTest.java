import domain.Key;
import domain.Record;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

public class WindowTest {
  Predicate<Record> trigger = new Predicate<Record>() {
    int current = 0;

    @Override
    public boolean test(Record record) {
      if( record.key.time > current){
        current = record.key.time;
        return true;
      }
      return false;
    }
  };

  Flux<Record> getSourceFlux() {
    return Flux.just(
        new Record(new Key(1, "A"), 1),
        new Record(new Key(1, "B"), 1),
        new Record(new Key(1, "A"), 2),
        new Record(new Key(2, "A"), 1),
        new Record(new Key(2, "C"), 1),
        new Record(new Key(2, "C"), 2)
    );
  }


  @Test
  public void window_flatmap(){
    Flux<Record> flux = getSourceFlux();

    Flux<Record> result = flux.windowUntil(trigger, true)
        .flatMap(window -> window.distinct(r -> r.key));

    StepVerifier.create(result)
        .expectNext(new Record(new Key(1, "A"), 1))
        .expectNext(new Record(new Key(1, "B"), 1))
        .expectNext(new Record(new Key(2, "A"), 1))
        .expectNext(new Record(new Key(2, "C"), 1))
        .expectComplete()
        .verify();
  }

  @Test
  public void window_flatmap_groupby_concatmap(){
    Flux<Record> flux = getSourceFlux();

    Flux<Record> result = flux.windowUntil(trigger, true)
        .flatMap(window ->
            window.groupBy(r -> r.key)
                .flatMap(groupedFlux ->
                    groupedFlux.reduce( (a,b)-> a.value > b.value ? a : b)));

    StepVerifier.create(result)
        .expectNext(new Record(new Key(1, "A"), 2))
        .expectNext(new Record(new Key(1, "B"), 1))
        .expectNext(new Record(new Key(2, "A"), 1))
        .expectNext(new Record(new Key(2, "C"), 2))
        .expectComplete()
        .verify();
  }

  @Test
  public void window_flatmap2(){
    Flux<String> colors = Flux.just("red", "green", "blue", "red", "yellow", "green", "green");
    Flux remastered = colors
        .window(2)
        .flatMap(window -> window.flatMap(Flux::just, Flux::error, () -> Flux.just("===")));

    StepVerifier.create(remastered)
        .expectNext("red", "green")
        .expectNext("===")
        .expectNext("blue", "red")
        .expectNext("===")
        .expectNext("yellow", "green")
        .expectNext("===")
        .expectNext("green")
        .expectNext("===")
        .expectComplete()
        .verify();
  }

}