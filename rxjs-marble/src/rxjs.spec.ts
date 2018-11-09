import { marbles } from "rxjs-marbles/mocha";
import { map } from "rxjs/operators";
import { throttleTime } from 'rxjs/operators';
import { merge } from 'rxjs';

describe("rxjs-marbles", () => {

    it("should support marble tests", marbles(m => {

        const source =  m.hot("--^-a-b-c-|");
        const subs =            "^-------!";
        const expected =        "--b-c-d-|";

        const destination = source.pipe(
            map(value => String.fromCharCode(value.charCodeAt(0) + 1))
        );
        m.expect(destination).toBeObservable(expected);
        m.expect(source).toHaveSubscriptions(subs);
    }));

    it("merge", marbles(m => {
        var e1 = m.hot('----a--^--b-------c--|');
        var e2 = m.hot(  '---d-^--e---------f-----|');
        var expected =      '---(be)----c-f-----|';
        
        m.expect(merge(e1, e2)).toBeObservable(expected);
    }));

    it("throttleTime", marbles(m => {
        const e1 =  m.cold('-a--b--c---|');
        const subs =       '^----------!';
        const expected =   '-a-----c---|';

        m.expect(e1.pipe(throttleTime(3))).toBeObservable(expected);
        m.expect(e1).toHaveSubscriptions(subs);
    }));

});