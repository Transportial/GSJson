/**
 * GSJson JavaScript test suite
 * Mirrors the Kotlin GSJsonTest.kt tests to verify full parity.
 */

import { get, set, exists, getResult, forEachLine, ResultType } from './GSJson.js';

// ─── shared test data (same as Kotlin GSJsonTest) ────────────────────────────

const selectJson = JSON.stringify({
  age: 37,
  children: ['Sara', 'Alex', 'Jack'],
  'fav.movie': 'Deer Hunter',
  friends: [
    { age: 44, first: 'Dale', last: 'Murphy' },
    { age: 68, first: 'Roger', last: 'Craig' },
    { age: 47, first: 'Jane', last: 'Murphy' },
    { age: 37, first: 'Sjenkie', last: 'Name' },
  ],
  name: { first: 'Tom', last: 'Anderson' },
});

// ─── testObjectSelectors ─────────────────────────────────────────────────────

test('testObjectSelectors', () => {
  const age = get(selectJson, 'age');
  expect(age).toBe(37);
  expect(age).not.toBe('37');

  const firstName = get(selectJson, 'name.first');
  expect(firstName).toBe('Tom');

  const favMovie = get(selectJson, 'fav\\.movie');
  expect(favMovie).toBe('Deer Hunter');
});

// ─── testArraySelectors ──────────────────────────────────────────────────────

test('testArraySelectors', () => {
  const children = get(selectJson, 'children');
  expect(Array.isArray(children)).toBe(true);
  expect(children).toEqual(['Sara', 'Alex', 'Jack']);

  const childOne = get(selectJson, 'children.[0]');
  expect(childOne).toBe('Sara');

  const childTwo = get(selectJson, 'children.[1]');
  expect(childTwo).toBe('Alex');

  const friendsOne = get(selectJson, 'friends.[0].first');
  expect(friendsOne).toBe('Dale');

  const friendAge47 = get(selectJson, 'friends.[age == "47"].[0].first');
  expect(friendAge47).toBe('Jane');

  // Back-reference: compare friend age to root "age" (37)
  const friendAtDynamicAge = get(selectJson, 'friends.[age == <<.age].[0].first');
  expect(friendAtDynamicAge).toBe('Sjenkie');

  const friendAgeAtDynamicAge = get(selectJson, 'friends.[age == <<.age].[0].age');
  expect(friendAgeAtDynamicAge).toBe(37);
  expect(friendAgeAtDynamicAge).not.toBe('37');
});

// ─── testSpecialSelectors ────────────────────────────────────────────────────

test('testSpecialSelectors — constant selector', () => {
  const constant = get(selectJson, '"value"');
  expect(constant).toBe('value');
});

// ─── settingObjectJsonValues ──────────────────────────────────────────────────

test('settingObjectJsonValues', () => {
  const age = set('', 'age', 37);
  expect(JSON.parse(age)).toEqual({ age: 37 });

  const firstName = set('', 'name.first', 'Tom');
  expect(JSON.parse(firstName)).toEqual({ name: { first: 'Tom' } });

  const favMovie = set('', 'fav\\.movie', 'Deer Hunter');
  expect(JSON.parse(favMovie)).toMatchObject({ 'fav.movie': 'Deer Hunter' });
});

// ─── settingArrayJsonValues ───────────────────────────────────────────────────

test('settingArrayJsonValues', () => {
  const children = set('', 'children', ['Sara', 'Alex', 'Jack']);
  expect(JSON.parse(children)).toEqual({ children: ['Sara', 'Alex', 'Jack'] });

  const childOne = set('', 'children.[0]', 'Sara');
  expect(JSON.parse(childOne)).toMatchObject({ children: ['Sara'] });

  const friendsOne = set('', 'friends.[0].first', 'Dale');
  expect(JSON.parse(friendsOne)).toEqual({ friends: [{ first: 'Dale' }] });
});

// ─── testWildcardSelectors ────────────────────────────────────────────────────

test('testWildcardSelectors', () => {
  const testData = { test1: 'value1', test2: 'value2', other: 'value3' };
  const testJson = JSON.stringify(testData);

  const allTestKeys = get(testJson, 'test*');
  expect(allTestKeys).not.toBeNull();
  expect(Array.isArray(allTestKeys)).toBe(true);

  const singleCharMatch = get(testJson, 'othe?');
  expect(singleCharMatch).toBe('value3');
});

// ─── testArrayLengthAndChildPaths ─────────────────────────────────────────────

test('testArrayLengthAndChildPaths', () => {
  const arrayLength = get(selectJson, 'children.[#]');
  expect(arrayLength).toBe(3);

  const allFirstNames = get(selectJson, 'friends.[#.first]');
  expect(allFirstNames).not.toBeNull();
  expect(Array.isArray(allFirstNames)).toBe(true);
  expect(allFirstNames).toEqual(['Dale', 'Roger', 'Jane', 'Sjenkie']);
});

// ─── testAdditionalComparisonOperators ───────────────────────────────────────

test('testAdditionalComparisonOperators', () => {
  const friendsOlderThan45 = get(selectJson, 'friends.[age > "45"].[0].first');
  expect(friendsOlderThan45).not.toBeNull();

  const friendsYoungerOrEqual44 = get(selectJson, 'friends.[age <= "44"].[0].first');
  expect(friendsYoungerOrEqual44).toBe('Dale');
});

// ─── testPatternMatching ──────────────────────────────────────────────────────

test('testPatternMatching', () => {
  const matchingNames = get(selectJson, 'friends.[first % "D*"].[0].first');
  expect(matchingNames).toBe('Dale');

  const notMatchingNames = get(selectJson, 'friends.[first !% "D*"].[0].first');
  expect(notMatchingNames).not.toBe('Dale');
});

// ─── testBuiltInModifiers ─────────────────────────────────────────────────────

test('testBuiltInModifiers', () => {
  const reversedChildren = get(selectJson, 'children|@reverse');
  expect(reversedChildren).not.toBeNull();
  expect(reversedChildren).toEqual(['Jack', 'Alex', 'Sara']);

  const keys = get(selectJson, 'name|@keys');
  expect(keys).not.toBeNull();
  expect(Array.isArray(keys)).toBe(true);

  const values = get(selectJson, 'name|@values');
  expect(values).not.toBeNull();
  expect(Array.isArray(values)).toBe(true);
});

// ─── testJsonLines ────────────────────────────────────────────────────────────

test('testJsonLines', () => {
  const jsonLinesData = [
    '{"name": "Alice", "age": 30}',
    '{"name": "Bob", "age": 25}',
    '{"name": "Charlie", "age": 35}',
  ].join('\n');

  const lineCount = get(jsonLinesData, '..#');
  expect(lineCount).toBe(3);

  const secondName = get(jsonLinesData, '..[1].name');
  expect(secondName).toBe('Bob');

  const allNames = get(jsonLinesData, '..#.name');
  expect(allNames).not.toBeNull();
  expect(Array.isArray(allNames)).toBe(true);
  expect(allNames).toEqual(['Alice', 'Bob', 'Charlie']);
});

// ─── testResultType ───────────────────────────────────────────────────────────

test('testResultType', () => {
  const result = getResult(selectJson, 'age');
  expect(result.int()).toBe(37);
  expect(result.string()).toBe('37');
  expect(result.exists).toBe(true);
  expect(result.type).toBe(ResultType.NUMBER);
});

// ─── testExistsMethod ─────────────────────────────────────────────────────────

test('testExistsMethod', () => {
  expect(exists(selectJson, 'age')).toBe(true);
  expect(exists(selectJson, 'nonexistent')).toBe(false);
});

// ─── testReducerModifiers ─────────────────────────────────────────────────────

test('testReducerModifiers', () => {
  const numbersJson = JSON.stringify({ scores: [10, 20, 30, 40, 50], prices: ['1.5', '2.0', '3.5'] });

  expect(get(numbersJson, 'scores|@sum')).toBe(150);
  expect(get(numbersJson, 'scores|@avg')).toBe(30);
  expect(get(numbersJson, 'scores|@min')).toBe(10);
  expect(get(numbersJson, 'scores|@max')).toBe(50);
  expect(get(numbersJson, 'scores|@count')).toBe(5);
  expect(get(numbersJson, 'scores|@join')).toBe('10,20,30,40,50');
  expect(get(numbersJson, 'scores|@join:-')).toBe('10-20-30-40-50');
  expect(get(numbersJson, 'prices|@sum')).toBe(7);
});

// ─── testReducerOnNestedArrays ────────────────────────────────────────────────

test('testReducerOnNestedArrays', () => {
  const totalAge = get(selectJson, 'friends.[#.age]|@sum');
  expect(totalAge).toBe(196);

  const avgAge = get(selectJson, 'friends.[#.age]|@avg');
  expect(avgAge).toBe(49);

  const allFirstNames = get(selectJson, 'friends.[#.first]|@join');
  expect(allFirstNames).toBe('Dale,Roger,Jane,Sjenkie');
});

// ─── testFallbackDefaultValues ────────────────────────────────────────────────

test('testFallbackDefaultValues', () => {
  expect(get(selectJson, 'age', 99)).toBe(37);
  expect(get(selectJson, 'name.first', 'Unknown')).toBe('Tom');
  expect(get(selectJson, 'nonexistent', 'default_value')).toBe('default_value');
  expect(get(selectJson, 'name.middle', 'J')).toBe('J');
  expect(get(selectJson, 'friends.[99].name', 'Not Found')).toBe('Not Found');
  expect(get(selectJson, 'missing.number', 42)).toBe(42);
  expect(get(selectJson, 'missing.boolean', true)).toBe(true);
  expect(get(selectJson, 'missing.list', ['a', 'b', 'c'])).toEqual(['a', 'b', 'c']);
});

// ─── testMathematicalOperations ───────────────────────────────────────────────

test('testMathematicalOperations', () => {
  const mathJson = JSON.stringify({
    numbers: [10, 20, 30],
    prices: ['1.5', '2.5', '3.0'],
    single: 15,
    negative: [-5, -10, 15],
  });

  expect(get(mathJson, 'numbers|@multiply:2')).toEqual([20, 40, 60]);
  expect(get(mathJson, 'numbers|@divide:2')).toEqual([5, 10, 15]);
  expect(get(mathJson, 'numbers|@add:5')).toEqual([15, 25, 35]);
  expect(get(mathJson, 'numbers|@subtract:5')).toEqual([5, 15, 25]);
  expect(get(mathJson, 'negative|@abs')).toEqual([5, 10, 15]);
  expect(get(mathJson, 'numbers|@add:10|@multiply:2')).toEqual([40, 60, 80]);
  expect(get(mathJson, 'prices|@multiply:2')).toEqual([3, 5, 6]);
});

// ─── testSortModifiers ────────────────────────────────────────────────────────

test('testSortModifiers', () => {
  expect(get(selectJson, 'children|@sort')).toEqual(['Alex', 'Jack', 'Sara']);
  expect(get(selectJson, 'children|@sort:desc')).toEqual(['Sara', 'Jack', 'Alex']);

  const numericJson = JSON.stringify({ numbers: [30, 10, 20, 5] });
  expect(get(numericJson, 'numbers|@sort')).toEqual([5, 10, 20, 30]);
  expect(get(numericJson, 'numbers|@sort:desc')).toEqual([30, 20, 10, 5]);

  // @sortBy:age ascending — youngest first = Sjenkie (37)
  const sortedByAge = get(selectJson, 'friends|@sortBy:age');
  expect(Array.isArray(sortedByAge)).toBe(true);
  expect(sortedByAge[0].first).toBe('Sjenkie');

  // @sortBy:age desc — oldest first = Roger (68)
  const sortedByAgeDesc = get(selectJson, 'friends|@sortBy:age:desc');
  expect(sortedByAgeDesc[0].first).toBe('Roger');

  // @sortBy:first ascending — Dale first
  const sortedByFirst = get(selectJson, 'friends|@sortBy:first');
  expect(sortedByFirst[0].first).toBe('Dale');

  // @sortBy:first desc — Sjenkie first
  const sortedByFirstDesc = get(selectJson, 'friends|@sortBy:first:desc');
  expect(sortedByFirstDesc[0].first).toBe('Sjenkie');

  // Gracefully handle missing property
  const sortedByMissing = get(selectJson, 'friends|@sortBy:missing');
  expect(sortedByMissing).not.toBeNull();

  // Non-array — return original
  const sortedObject = get(selectJson, 'name|@sort');
  expect(sortedObject).not.toBeNull();
});

// ─── testSortChaining ─────────────────────────────────────────────────────────

test('testSortChaining', () => {
  const chainedSort = get(selectJson, 'friends.[age > "40"]|@sortBy:age|[#.first]');
  expect(chainedSort).not.toBeNull();
  expect(Array.isArray(chainedSort)).toBe(true);

  const highestAge = get(selectJson, 'friends.[#.age]|@sort:desc|[0]');
  expect(highestAge).toBe(68);

  const lowestAge = get(selectJson, 'friends.[#.age]|@sort|[0]');
  expect(lowestAge).toBe(37);

  const reversedSorted = get(selectJson, 'children|@sort|@reverse');
  expect(reversedSorted).toEqual(['Sara', 'Jack', 'Alex']);
});

// ─── testDynamicMathOperations ────────────────────────────────────────────────

test('testDynamicMathOperations', () => {
  const dynamicMathJson = JSON.stringify({
    numbers: [10, 20, 30],
    multiplier: 3,
    divisor: 2,
    addend: 5,
    subtrahend: 8,
    exponent: 2,
    precision: 1,
    config: { factor: 4, offset: 15 },
  });

  expect(get(dynamicMathJson, 'numbers|@multiply:multiplier')).toEqual([30, 60, 90]);
  expect(get(dynamicMathJson, 'numbers|@divide:divisor')).toEqual([5, 10, 15]);
  expect(get(dynamicMathJson, 'numbers|@add:addend')).toEqual([15, 25, 35]);
  expect(get(dynamicMathJson, 'numbers|@subtract:subtrahend')).toEqual([2, 12, 22]);
  expect(get(dynamicMathJson, 'numbers|@power:exponent')).toEqual([100, 400, 900]);

  // Dynamic round precision
  const rounded = get(dynamicMathJson, 'numbers|@divide:3|@round:precision');
  expect(rounded[0]).toBeCloseTo(3.3, 1);
  expect(rounded[1]).toBeCloseTo(6.7, 1);
  expect(rounded[2]).toBeCloseTo(10.0, 1);

  // Nested config path
  expect(get(dynamicMathJson, 'numbers|@multiply:config.factor')).toEqual([40, 80, 120]);

  // Chained dynamic ops
  const chained = get(dynamicMathJson, 'numbers|@add:config.offset|@divide:divisor');
  expect(chained).toEqual([12.5, 17.5, 22.5]);
});

// ─── testDynamicMathWithSingleValues ──────────────────────────────────────────

test('testDynamicMathWithSingleValues', () => {
  const arrayJson = JSON.stringify({
    values: [50],
    multiplier: 1.5,
    divisor: 2.5,
    addend: 25,
    subtrahend: 10,
    exponent: 3,
  });

  expect(get(arrayJson, 'values|@multiply:multiplier')).toEqual([75]);
  expect(get(arrayJson, 'values|@divide:divisor')).toEqual([20]);
  expect(get(arrayJson, 'values|@add:addend')).toEqual([75]);
  expect(get(arrayJson, 'values|@subtract:subtrahend')).toEqual([40]);
  expect(get(arrayJson, 'values|@power:exponent')).toEqual([125000]);
});

// ─── testDynamicMathEdgeCases ─────────────────────────────────────────────────

test('testDynamicMathEdgeCases', () => {
  const edgeCaseJson = JSON.stringify({
    numbers: [10, 20, 30],
    zero: 0,
    negative: -5,
    decimal: 2.5,
    missing: null,
    nonNumeric: 'not_a_number',
  });

  // Division by zero — return original
  const divByZero = get(edgeCaseJson, 'numbers|@divide:zero');
  expect(divByZero).toEqual([10, 20, 30]);

  // Negative multiplier
  expect(get(edgeCaseJson, 'numbers|@multiply:negative')).toEqual([-50, -100, -150]);

  // Decimal divisor
  expect(get(edgeCaseJson, 'numbers|@divide:decimal')).toEqual([4, 8, 12]);

  // Missing selector → default (1.0 for multiply)
  expect(get(edgeCaseJson, 'numbers|@multiply:nonExistentField')).toEqual([10, 20, 30]);

  // Non-numeric selector → default (0.0 for add)
  expect(get(edgeCaseJson, 'numbers|@add:nonNumeric')).toEqual([10, 20, 30]);
});

// ─── testDynamicMathWithArraySelectors ───────────────────────────────────────

test('testDynamicMathWithArraySelectors', () => {
  const arrayMathJson = JSON.stringify({
    data: [
      { values: [1, 2, 3], multiplier: 10 },
      { values: [4, 5, 6], multiplier: 20 },
    ],
    globalMultiplier: 3,
  });

  expect(get(arrayMathJson, 'data.[0].values|@multiply:globalMultiplier')).toEqual([3, 6, 9]);
  expect(get(arrayMathJson, 'data.[1].values|@multiply:globalMultiplier')).toEqual([12, 15, 18]);
});

// ─── testForEachLine ──────────────────────────────────────────────────────────

test('testForEachLine', () => {
  const jsonLinesData = [
    '{"name": "Alice", "age": 30}',
    '{"name": "Bob", "age": 25}',
    '{"name": "Charlie", "age": 35}',
  ].join('\n');

  const names = [];
  forEachLine(jsonLinesData, (result) => {
    names.push(result.value.name);
    return true; // continue
  });
  expect(names).toEqual(['Alice', 'Bob', 'Charlie']);

  // Early stop
  const partial = [];
  forEachLine(jsonLinesData, (result) => {
    partial.push(result.value.name);
    return partial.length < 2; // stop after 2
  });
  expect(partial).toEqual(['Alice', 'Bob']);
});

// ─── testResultTypeFull ───────────────────────────────────────────────────────

test('testResultTypeFull — ResultType enum and array()', () => {
  const strResult = getResult(selectJson, 'name.first');
  expect(strResult.type).toBe(ResultType.STRING);
  expect(strResult.string()).toBe('Tom');
  expect(strResult.exists).toBe(true);

  const numResult = getResult(selectJson, 'age');
  expect(numResult.type).toBe(ResultType.NUMBER);
  expect(numResult.int()).toBe(37);
  expect(numResult.double()).toBe(37);

  const missingResult = getResult(selectJson, 'nonexistent');
  expect(missingResult.exists).toBe(false);
  expect(missingResult.type).toBe(ResultType.NULL);

  const arrayResult = getResult(selectJson, 'friends.[#.first]');
  expect(arrayResult.type).toBe(ResultType.ARRAY);
  const items = arrayResult.array();
  expect(items.length).toBe(4);
  expect(items[0].string()).toBe('Dale');

  // forEach
  const collected = [];
  arrayResult.forEach((r) => collected.push(r.string()));
  expect(collected).toEqual(['Dale', 'Roger', 'Jane', 'Sjenkie']);
});

// ─── testModifiers — @tostr, @fromstr, @this, @pretty, @ugly ─────────────────

test('testModifiers — @tostr @fromstr @this @pretty @ugly @flatten', () => {
  // @this — returns value unchanged
  expect(get(selectJson, 'age|@this')).toBe(37);

  // @pretty — returns formatted JSON string
  const pretty = get(selectJson, 'name|@pretty');
  expect(typeof pretty).toBe('string');
  expect(pretty).toContain('\n');

  // @ugly — returns compact JSON string
  const ugly = get(selectJson, 'name|@ugly');
  expect(typeof ugly).toBe('string');
  expect(ugly).not.toContain('\n');

  // @tostr — serialises value to JSON string
  const tostr = get(selectJson, 'children|@tostr');
  expect(typeof tostr).toBe('string');
  expect(JSON.parse(tostr)).toEqual(['Sara', 'Alex', 'Jack']);

  // @fromstr — deserialises a JSON string field back to an object
  const withStringField = JSON.stringify({ data: '{"key":"val"}' });
  const fromstr = get(withStringField, 'data|@fromstr');
  expect(fromstr).toEqual({ key: 'val' });

  // @flatten
  const nestedJson = JSON.stringify({ arr: [[1, 2], [3, 4], [5]] });
  expect(get(nestedJson, 'arr|@flatten')).toEqual([1, 2, 3, 4, 5]);
});

// ─── testNestedMultiQuery ─────────────────────────────────────────────────────

test('testNestedMultiQuery — friends.[nets.[# == "fb"]]', () => {
  const json = JSON.stringify({
    friends: [
      { first: 'Dale', nets: ['ig', 'fb', 'tw'] },
      { first: 'Roger', nets: ['fb', 'tw'] },
      { first: 'Jane', nets: ['ig', 'tw'] },
      { first: 'David', nets: ['fb'] },
    ],
  });

  // Filter friends who have "fb" in their nets array
  const withFb = get(json, 'friends.[nets.[# == "fb"]].[#.first]');
  expect(withFb).toEqual(['Dale', 'Roger', 'David']);
});

// ─── testCountOnObject ────────────────────────────────────────────────────────

test('testCountOnObject — @count works on objects too', () => {
  const obj = JSON.stringify({ name: { first: 'Tom', last: 'Anderson' } });
  const count = get(obj, 'name|@count');
  expect(count).toBe(2);
});

// ─── testFlatMapModifier ──────────────────────────────────────────────────────

test('testFlatMapModifier', () => {
  const ordersJson = JSON.stringify({
    orders: [
      { id: 1, items: ['apple', 'banana'] },
      { id: 2, items: ['cherry'] },
      { id: 3, items: ['date', 'elderberry'] },
    ],
  });

  // sub-arrays are flattened one level
  expect(get(ordersJson, 'orders|@flatMap:items')).toEqual(['apple', 'banana', 'cherry', 'date', 'elderberry']);

  // scalar values are collected (one per item)
  expect(get(selectJson, 'friends|@flatMap:first')).toEqual(['Dale', 'Roger', 'Jane', 'Sjenkie']);

  // items where the path is missing are omitted
  const sparseJson = JSON.stringify({
    items: [
      { name: 'a', tags: ['x', 'y'] },
      { name: 'b' },
      { name: 'c', tags: ['z'] },
    ],
  });
  expect(get(sparseJson, 'items|@flatMap:tags')).toEqual(['x', 'y', 'z']);

  // non-array input returns []
  expect(get(selectJson, 'name|@flatMap:first')).toEqual([]);
});

// ─── testFilterModifier ───────────────────────────────────────────────────────

test('testFilterModifier', () => {
  // equality filter
  const murphys = get(selectJson, 'friends|@filter:last == "Murphy"');
  expect(Array.isArray(murphys)).toBe(true);
  expect(murphys).toHaveLength(2);
  expect(murphys.map((f) => f.first)).toEqual(['Dale', 'Jane']);

  // numeric comparison
  const older = get(selectJson, 'friends|@filter:age > "45"');
  expect(older).toHaveLength(2);
  expect(older.map((f) => f.first)).toEqual(['Roger', 'Jane']);

  // wildcard / like operator
  const dNames = get(selectJson, 'friends|@filter:first % "D*"');
  expect(dNames).toHaveLength(1);
  expect(dNames[0].first).toBe('Dale');

  // chain: filter then extract field list
  const names = get(selectJson, 'friends|@filter:last == "Murphy"|[#.first]');
  expect(names).toEqual(['Dale', 'Jane']);

  // non-array input is returned unchanged
  const notArray = get(selectJson, 'name|@filter:first == "Tom"');
  expect(notArray).toEqual({ first: 'Tom', last: 'Anderson' });
});

// ─── testGroupByModifier ──────────────────────────────────────────────────────

test('testGroupByModifier', () => {
  // group friends by last name — 3 distinct last names
  const groups = get(selectJson, 'friends|@groupBy:last');
  expect(Array.isArray(groups)).toBe(true);
  expect(groups).toHaveLength(3);

  // each group is itself an array
  groups.forEach((g) => expect(Array.isArray(g)).toBe(true));

  // total members across all groups equals the original array length
  expect(groups.reduce((sum, g) => sum + g.length, 0)).toBe(4);

  // the Murphy group has 2 members and preserves insertion order
  const murphyGroup = groups.find((g) => g[0].last === 'Murphy');
  expect(murphyGroup).not.toBeUndefined();
  expect(murphyGroup).toHaveLength(2);
  expect(murphyGroup.map((f) => f.first)).toEqual(['Dale', 'Jane']);

  // non-array input is wrapped in a single-element outer array
  const nonArray = get(selectJson, 'name|@groupBy:first');
  expect(Array.isArray(nonArray)).toBe(true);
  expect(nonArray).toHaveLength(1);
});

// ─── testJoinQuotedSeparator ──────────────────────────────────────────────────

test('testJoinQuotedSeparator — @join strips surrounding quotes from separator', () => {
  const numbersJson = JSON.stringify({ scores: [10, 20, 30, 40, 50] });

  // quoted multi-char separator: @join:", "
  expect(get(numbersJson, 'scores|@join:", "')).toBe('10, 20, 30, 40, 50');

  // quoted separator with spaces: @join:" | "
  expect(get(numbersJson, 'scores|@join:" | "')).toBe('10 | 20 | 30 | 40 | 50');

  // unquoted single-char separator still works: @join:-
  expect(get(numbersJson, 'scores|@join:-')).toBe('10-20-30-40-50');

  // no separator defaults to comma
  expect(get(numbersJson, 'scores|@join')).toBe('10,20,30,40,50');
});

// ─── testUniqueDistinctModifier ───────────────────────────────────────────────

test('testUniqueDistinctModifier', () => {
  const dupJson = JSON.stringify({ tags: ['a', 'b', 'a', 'c', 'b', 'c'] });

  // @unique removes duplicates, preserves insertion order
  expect(get(dupJson, 'tags|@unique')).toEqual(['a', 'b', 'c']);

  // @distinct is an alias
  expect(get(dupJson, 'tags|@distinct')).toEqual(['a', 'b', 'c']);

  // works on numeric arrays
  const numJson = JSON.stringify({ values: [1, 2, 2, 3, 1, 4] });
  expect(get(numJson, 'values|@unique')).toEqual([1, 2, 3, 4]);

  // non-array input is returned unchanged
  expect(get(selectJson, 'name|@unique')).toEqual({ first: 'Tom', last: 'Anderson' });

  // chained: extract all last names, deduplicate
  const lastNames = get(selectJson, 'friends|@unique');
  expect(lastNames).not.toBeNull(); // friends array has no dupes, all 4 remain
  expect(lastNames).toHaveLength(4);
});
