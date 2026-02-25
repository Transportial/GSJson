/**
 * GSJson - JavaScript port of the GSJson getter/setter syntax
 * Compatible with the Kotlin GSJson library used in the backend.
 *
 * Supported path features:
 *   - Dot notation:           name.first
 *   - Escaped dots:           fav\.movie
 *   - Constant value:         "value"  (returns the literal string between quotes)
 *   - Array index:            children.[0]
 *   - Array length:           children.[#]
 *   - All child values:       friends.[#.firstName]
 *   - Wildcards:              *.name  /  name.fir?t
 *   - Filtering:              friends.[age > "40"].first
 *                             friends.[last == "Murphy"].first
 *                             friends.[first % "D*"].last        (like / glob)
 *                             friends.[first !% "D*"].[0].first  (not-like)
 *   - Back-references:        friends.[age == <<.age].[0].first  (cross-ref parent)
 *   - Nested multi-query:     friends.[nets.[# == "fb"]].[#.first]
 *   - Modifiers (pipe):       children|@reverse  children|@sort  children|@join
 *                             friends.[#.age]|@sum  |@avg  |@min  |@max
 *                             name|@keys  name|@values  name|@count
 *                             children|@sort:desc  friends|@sortBy:age  |@sortBy:age:desc
 *                             .|@multiply:2  .|@add:5  etc.
 *                             name|@tostr  .|@fromstr  .|@this
 *                             .|@flatMap:path  .|@filter:field op value  .|@groupBy:field
 *   - JSON Lines:             ..#  ..[1].name  ..#.name
 *   - Default values:         GSJson.get(data, "missing.field", "default")
 *
 * Public API (mirrors the Kotlin GSJson object):
 *   GSJson.get(data, path, defaultValue?)   -> any
 *   GSJson.set(data, path, value)           -> string (JSON)
 *   GSJson.exists(data, path)               -> boolean
 *   GSJson.getResult(data, path)            -> GSJsonResult
 *   GSJson.forEachLine(jsonLines, fn)       -> void  (fn receives GSJsonResult, return false to stop)
 */

// ─── type helpers ────────────────────────────────────────────────────────────

const isObject = (v) => v !== null && typeof v === 'object' && !Array.isArray(v);
const isArray  = (v) => Array.isArray(v);

// ─── wildcard helpers ────────────────────────────────────────────────────────

/** Convert a wildcard pattern (*, ?) to a RegExp */
const wildcardToRegex = (pattern) => {
  const escaped = pattern
    .replace(/[.+^${}()|[\]\\]/g, '\\$&')
    .replace(/\*/g, '.*')
    .replace(/\?/g, '.');
  return new RegExp(`^${escaped}$`);
};

const matchesWildcard = (key, pattern) => wildcardToRegex(pattern).test(key);

// ─── modifier argument resolver ──────────────────────────────────────────────

/**
 * Resolve a modifier argument: numeric literal or a GSJson path into rootData.
 * Returns undefined when the arg is absent/missing (so callers can apply their own defaults).
 * Mirrors Kotlin's resolveArgument().
 */
const _resolveArg = (arg, rootData) => {
  if (arg === undefined || arg === null || arg === '') return undefined;
  const num = Number(arg);
  if (!isNaN(num)) return num;
  if (rootData !== undefined) {
    const val = _getByPath(rootData, arg, rootData, []);
    const resolved = Number(val);
    if (!isNaN(resolved)) return resolved;
    return undefined; // non-numeric / missing path
  }
  return undefined;
};

// ─── modifier execution ──────────────────────────────────────────────────────

const _applyModifier = (value, modifierExpr, rootData) => {
  const colonIdx = modifierExpr.indexOf(':');
  const mod = colonIdx === -1 ? modifierExpr : modifierExpr.slice(0, colonIdx);
  const arg = colonIdx === -1 ? undefined : modifierExpr.slice(colonIdx + 1).trim();

  switch (mod) {
    // ── array / object transforms ──────────────────────────────────────────
    case '@reverse':
      if (isArray(value)) return [...value].reverse();
      if (isObject(value)) {
        const rev = {};
        Object.keys(value).reverse().forEach((k) => { rev[k] = value[k]; });
        return rev;
      }
      return value;

    case '@sort':
      if (!isArray(value)) return value;
      return [...value].sort((a, b) => {
        const cmp = typeof a === 'number' && typeof b === 'number'
          ? a - b
          : String(a).localeCompare(String(b));
        return arg === 'desc' ? -cmp : cmp;
      });

    case '@sortBy': {
      if (!isArray(value)) return value;
      const colonPos = arg ? arg.indexOf(':') : -1;
      const field = colonPos === -1 ? arg : arg.slice(0, colonPos);
      const dir   = colonPos === -1 ? 'asc' : arg.slice(colonPos + 1).toLowerCase();
      if (!field) return value;
      return [...value].sort((a, b) => {
        const va = a?.[field];
        const vb = b?.[field];
        let cmp;
        if (typeof va === 'number' && typeof vb === 'number') cmp = va - vb;
        else if (va == null && vb != null) cmp = -1;
        else if (va != null && vb == null) cmp = 1;
        else if (va == null && vb == null) cmp = 0;
        else cmp = String(va).localeCompare(String(vb));
        return dir === 'desc' ? -cmp : cmp;
      });
    }

    case '@flatten':
      return isArray(value) ? value.flat(Infinity) : value;

    case '@keys':
      return isObject(value) ? Object.keys(value) : [];

    case '@values':
      return isObject(value) ? Object.values(value) : [];

    case '@this':
      return value;

    // ── formatting ─────────────────────────────────────────────────────────
    case '@pretty':
      return JSON.stringify(value, null, 2);

    case '@ugly':
      return JSON.stringify(value);

    case '@tostr':
      return JSON.stringify(value);

    case '@fromstr': {
      const src = typeof value === 'string' ? value : JSON.stringify(value);
      try { return JSON.parse(src); } catch { return value; }
    }

    // ── aggregators ────────────────────────────────────────────────────────
    case '@count':
    case '@length':
      if (isArray(value)) return value.length;
      if (isObject(value)) return Object.keys(value).length;
      return 1;

    case '@sum':
      return isArray(value) ? value.reduce((s, v) => s + Number(v), 0) : 0;

    case '@avg': {
      if (!isArray(value) || value.length === 0) return 0;
      return value.reduce((s, v) => s + Number(v), 0) / value.length;
    }

    case '@min':
      return isArray(value) ? Math.min(...value.map(Number)) : value;

    case '@max':
      return isArray(value) ? Math.max(...value.map(Number)) : value;

    case '@join': {
      // Strip surrounding quotes so @join:", " and @join:" | " work as expected.
      const sep = (arg !== undefined && arg !== '') ? arg.replace(/^["']|["']$/g, '') : ',';
      return isArray(value) ? value.join(sep) : String(value ?? '');
    }

    case '@unique':
    case '@distinct': {
      // Remove duplicate primitive values (strings, numbers) from an array.
      if (!isArray(value)) return value;
      return [...new Set(value)];
    }

    // ── new modifiers ──────────────────────────────────────────────────────

    case '@flatMap': {
      // Maps over each array item using a GSJson path, then flattens one level.
      // Undefined / null results are omitted.
      // Example:  actions|@flatMap:entity.actions
      if (!isArray(value)) return [];
      return value.flatMap((item) => {
        const result = _getByPath(item, arg, rootData, []);
        if (result === undefined || result === null) return [];
        return isArray(result) ? result : [result];
      });
    }

    case '@filter': {
      // Filters array elements by a field condition: @filter:field op value
      // Example:  @filter:type == "transportEquipment"
      if (!isArray(value)) return value;
      const fmatch = arg.match(/^(.+?)\s*(==|!=|<=|>=|<|>|!%|%)\s*(.+)$/);
      if (!fmatch) return value;
      const [, fieldExpr, op, rawRight] = fmatch;
      const right = rawRight.trim().replace(/^["']|["']$/g, '');
      return value.filter((item) =>
        _compareValues(_getByPath(item, fieldExpr.trim(), rootData, []), op, right)
      );
    }

    case '@groupBy': {
      // Groups an array by a key path.  Returns an array of arrays (each
      // sub-array contains items sharing the same key value), preserving
      // insertion order of groups.
      // Usage:  actions|@groupBy:entity.groupId
      if (!isArray(value)) return [value];
      const groups = new Map();
      for (const item of value) {
        const groupKey = String(_getByPath(item, arg, rootData, []) ?? '');
        if (!groups.has(groupKey)) groups.set(groupKey, []);
        groups.get(groupKey).push(item);
      }
      return Array.from(groups.values());
    }

    // ── math ───────────────────────────────────────────────────────────────
    case '@multiply':
    case '@mul': {
      const factor = _resolveArg(arg, rootData) ?? 1.0; // default 1.0 (neutral for multiply)
      return isArray(value) ? value.map((v) => Number(v) * factor) : Number(value) * factor;
    }

    case '@divide':
    case '@div': {
      const divisor = _resolveArg(arg, rootData) ?? 1.0; // default 1.0 (neutral for divide)
      if (divisor === 0) return value; // mirrors Kotlin: return original on div/0
      return isArray(value) ? value.map((v) => Number(v) / divisor) : Number(value) / divisor;
    }

    case '@add':
    case '@plus': {
      const addend = _resolveArg(arg, rootData) ?? 0.0; // default 0.0 (neutral for add)
      return isArray(value) ? value.map((v) => Number(v) + addend) : Number(value) + addend;
    }

    case '@subtract':
    case '@sub': {
      const sub = _resolveArg(arg, rootData) ?? 0.0; // default 0.0 (neutral for subtract)
      return isArray(value) ? value.map((v) => Number(v) - sub) : Number(value) - sub;
    }

    case '@power':
    case '@pow': {
      const exp = _resolveArg(arg, rootData) ?? 1.0; // default 1.0 (neutral for power)
      return isArray(value) ? value.map((v) => Math.pow(Number(v), exp)) : Math.pow(Number(value), exp);
    }

    case '@abs':
      return isArray(value) ? value.map((v) => Math.abs(Number(v))) : Math.abs(Number(value));

    case '@round': {
      const precision = (arg !== undefined ? _resolveArg(arg, rootData) : undefined) ?? 0;
      const f = Math.pow(10, precision);
      return isArray(value)
        ? value.map((v) => Math.round(Number(v) * f) / f)
        : Math.round(Number(value) * f) / f;
    }

    default:
      return value;
  }
};

// ─── path tokeniser ──────────────────────────────────────────────────────────

/**
 * Split a GSJson path into instructions, respecting:
 *   - escaped dots (\.)
 *   - bracket segments [...]
 *   - pipe characters | (treated as instruction separators, like dots)
 *
 * Mirrors Kotlin's selectionToInstructions().
 */
const _tokenizePath = (path) => {
  const tokens = [];
  let current = '';
  let i = 0;

  while (i < path.length) {
    const ch = path[i];

    // Escaped dot → literal dot
    if (ch === '\\' && i + 1 < path.length && path[i + 1] === '.') {
      current += '.';
      i += 2;
      continue;
    }

    // Bracket group [...]
    if (ch === '[') {
      if (current) { tokens.push(current); current = ''; }
      let bracket = '[';
      let depth = 1;
      i++;
      while (i < path.length && depth > 0) {
        if (path[i] === '[') depth++;
        if (path[i] === ']') depth--;
        if (depth > 0) bracket += path[i];
        i++;
      }
      bracket += ']';
      tokens.push(bracket);
      if (path[i] === '.') i++; // skip separator dot
      continue;
    }

    // Dot separator (not escaped)
    if (ch === '.') {
      if (current) { tokens.push(current); current = ''; }
      i++;
      continue;
    }

    // Pipe — flush and collect the full modifier instruction up to the next top-level pipe
    if (ch === '|') {
      if (current) { tokens.push(current); current = ''; }
      i++;
      let mod = '';
      let bracketDepth = 0;
      while (i < path.length) {
        if (path[i] === '[') bracketDepth++;
        if (path[i] === ']') bracketDepth--;
        if (path[i] === '|' && bracketDepth === 0) break;
        mod += path[i];
        i++;
      }
      if (mod) tokens.push(mod.trim());
      continue;
    }

    current += ch;
    i++;
  }

  if (current) tokens.push(current);
  return tokens.filter((t) => t !== '');
};

// ─── bracket evaluation ───────────────────────────────────────────────────────

/**
 * Evaluate a bracket token like [0], [#], [#.field], [age > "40"], [nets.[# == "fb"]]
 * contextNodes is the list of previously visited nodes for back-references.
 */
const _evalBracket = (inner, value, rootData, contextNodes) => {
  inner = inner.trim();

  // [#] — length
  if (inner === '#') {
    return isArray(value) ? value.length : 0;
  }

  // [#.field] — extract child field from every element
  if (inner.startsWith('#.')) {
    const field = inner.slice(2);
    if (!isArray(value)) return [];
    return value
      .map((item) => _getByPath(item, field, rootData, contextNodes))
      .filter((v) => v !== undefined);
  }

  // Numeric index
  if (/^-?\d+$/.test(inner)) {
    const idx = parseInt(inner, 10);
    if (isArray(value)) return idx >= 0 ? value[idx] : value[value.length + idx];
    return undefined;
  }

  // Nested multi-query: #(subQuery)# — filter where subQuery holds
  if (inner.startsWith('#(') && inner.endsWith(')#')) {
    const subQuery = inner.slice(2, -2);
    if (!isArray(value)) return value;
    return value.filter((item) => _evaluateNestedQuery(item, subQuery, rootData, contextNodes));
  }

  // Nested sub-path filter: [subPath.[# == "val"]] or [subPath.[field op val]]
  // Triggered when inner contains a sub-bracket with no top-level comparison operator.
  if (inner.includes('[')) {
    let depth = 0;
    const topLevelOpRe = /(==|!=|<=|>=|<|>|!%|%)/;
    let outerStr = '';
    for (let ci = 0; ci < inner.length; ci++) {
      if (inner[ci] === '[') depth++;
      else if (inner[ci] === ']') depth--;
      else if (depth === 0) outerStr += inner[ci];
    }
    if (!topLevelOpRe.test(outerStr) && isArray(value)) {
      return value.filter((item) => {
        const sub = _getByPath(item, inner, rootData, contextNodes);
        if (isArray(sub)) return sub.length > 0;
        return sub !== undefined && sub !== null && sub !== false;
      });
    }
  }

  // Filter expression: field OP right  (right may be "quoted" or a back-reference)
  const filterMatch = inner.match(/^(.+?)\s*(==|!=|<=|>=|<|>|!%|%)\s*(.+)$/);
  if (filterMatch) {
    const [, fieldExpr, op, rawRight] = filterMatch;
    const fieldTrimmed = fieldExpr.trim();

    const resolveRight = (raw) => {
      const trimmed = raw.trim();
      if (trimmed.startsWith('<<')) {
        const depth = trimmed.split('').filter((c) => c === '<').length;
        const backNode = contextNodes[contextNodes.length - depth] ?? rootData;
        const subPath = trimmed.replace(/^<+/, '').replace(/^\./, '');
        return subPath ? _getByPath(backNode, subPath, rootData, contextNodes) : backNode;
      }
      return trimmed.replace(/^["']|["']$/g, '');
    };

    if (!isArray(value)) return value;

    return value.filter((item) => {
      let left;
      if (fieldTrimmed === '#') {
        left = item; // [# == "val"] — check if item equals value
      } else {
        left = _getByPath(item, fieldTrimmed, rootData, contextNodes);
      }
      return _compareValues(left, op, resolveRight(rawRight));
    });
  }

  // [*] — all elements
  if (inner === '*') return value;

  return undefined;
};

const _compareValues = (left, op, right) => {
  const lStr = String(left ?? '');
  const lNum = Number(left);
  const rNum = Number(right);
  const numericOk = !isNaN(lNum) && !isNaN(rNum);

  switch (op) {
    case '==': return lStr === String(right);
    case '!=': return lStr !== String(right);
    case '>':  return numericOk ? lNum > rNum : lStr > String(right);
    case '>=': return numericOk ? lNum >= rNum : lStr >= String(right);
    case '<':  return numericOk ? lNum < rNum : lStr < String(right);
    case '<=': return numericOk ? lNum <= rNum : lStr <= String(right);
    case '%':  return wildcardToRegex(String(right)).test(lStr);
    case '!%': return !wildcardToRegex(String(right)).test(lStr);
    default:   return false;
  }
};

/**
 * Evaluate nested multi-query patterns.
 * e.g.  nets.#(=="fb")  inside  friends.[nets.[# == "fb"]]
 */
const _evaluateNestedQuery = (element, query, rootData, contextNodes) => {
  const filterMatch = query.match(/^(.+?)\s*(==|!=|<=|>=|<|>|!%|%)\s*(.+)$/);
  if (filterMatch) {
    const [, fieldExpr, op, rawRight] = filterMatch;
    const left = _getByPath(element, fieldExpr.trim(), rootData, contextNodes);
    const right = rawRight.trim().replace(/^["']|["']$/g, '');
    return _compareValues(left, op, right);
  }
  const dotPos = query.indexOf('.');
  if (dotPos !== -1) {
    const subPath = query.slice(0, dotPos);
    const rest    = query.slice(dotPos + 1);
    const sub = _getByPath(element, subPath, rootData, contextNodes);
    if (isArray(sub)) {
      return sub.some((el) => _evaluateNestedQuery(el, rest, rootData, contextNodes));
    }
    return _evaluateNestedQuery(sub, rest, rootData, contextNodes);
  }
  return false;
};

// ─── core path traversal ─────────────────────────────────────────────────────

/**
 * Traverse `data` using a GSJson path (may include pipes for modifiers).
 * rootData is the top-level document, used for dynamic modifier args and back-refs.
 * contextNodes accumulates previously visited nodes for back-references.
 */
const _getByPath = (data, path, rootData, contextNodes = []) => {
  if (path === '' || path === '.') return data;

  // Constant selector: "value" → literal string
  if (path.startsWith('"') && path.endsWith('"') && path.length >= 2) {
    return path.slice(1, -1);
  }

  // JSON Lines selection
  if (path.startsWith('..')) {
    return _handleJsonLines(data, path, rootData);
  }

  const tokens = _tokenizePath(path);
  let current = data;
  const visited = [...contextNodes];

  for (const token of tokens) {
    if (current === null || current === undefined) return undefined;

    // Constant token
    if (token.startsWith('"') && token.endsWith('"') && token.length >= 2) {
      return token.slice(1, -1);
    }

    // Back-reference: one or more '<' characters
    if (/^<+$/.test(token)) {
      const depth = token.length;
      current = visited[visited.length - depth] ?? rootData;
      continue;
    }

    // Modifier (starts with @)
    if (token.startsWith('@')) {
      current = _applyModifier(current, token, rootData);
      continue;
    }

    // Bracket token
    if (token.startsWith('[') && token.endsWith(']')) {
      const inner = token.slice(1, -1);
      visited.push(current);
      current = _evalBracket(inner, current, rootData, visited);
      continue;
    }

    // Wildcard key match
    if (token.includes('*') || token.includes('?')) {
      if (isObject(current)) {
        const matched = Object.keys(current)
          .filter((k) => matchesWildcard(k, token))
          .map((k) => current[k]);
        visited.push(current);
        current = matched.length === 1 ? matched[0] : matched;
      } else {
        current = undefined;
      }
      continue;
    }

    // Normal key
    visited.push(current);
    current = (isObject(current) || isArray(current)) ? current[token] : undefined;
  }

  return current;
};

// ─── JSON Lines ───────────────────────────────────────────────────────────────

/**
 * Handle selections that start with '..' (JSON Lines).
 * Mirrors Kotlin handleJsonLines().
 */
const _handleJsonLines = (data, selection, rootData) => {
  const src = typeof data === 'string' ? data : JSON.stringify(data);
  const lines = src.trim().split('\n').map((l) => l.trim()).filter(Boolean);
  const arr = [];
  for (const line of lines) {
    try { arr.push(JSON.parse(line)); } catch { /* skip invalid */ }
  }

  const rest = selection.slice(2); // remove '..'

  if (rest === '' || rest === '#') {
    return arr.length;
  }

  // ..#.field — extract field from every line
  if (rest.startsWith('#.')) {
    const field = rest.slice(2);
    return arr.map((item) => _getByPath(item, field, rootData, [])).filter((v) => v !== undefined);
  }

  // ..[n].something — indexed access
  if (/^\d/.test(rest)) {
    return _getByPath(arr, `[${rest}]`, rootData, []);
  }

  // fallback: treat rest as a normal path on the array
  return _getByPath(arr, rest, rootData, []);
};

// ─── set ──────────────────────────────────────────────────────────────────────

/**
 * Set a value in a (parsed) object at the given path.
 * Returns the mutated clone as a JSON string.
 */
const _setByPath = (obj, path, value) => {
  const tokens = _tokenizePath(path).filter((t) => !t.startsWith('@'));
  if (tokens.length === 0) return obj;

  // Deep-clone to avoid mutating the original
  let root = JSON.parse(JSON.stringify(obj ?? {}));
  let current = root;

  for (let i = 0; i < tokens.length - 1; i++) {
    const token = tokens[i];
    const next  = tokens[i + 1];
    const isNextArray = next.startsWith('[') && next.endsWith(']') && /^\d+$/.test(next.slice(1, -1));

    if (token.startsWith('[') && token.endsWith(']')) {
      const idx = parseInt(token.slice(1, -1), 10);
      if (!isArray(current)) break;
      if (current[idx] === undefined || current[idx] === null) {
        current[idx] = isNextArray ? [] : {};
      }
      current = current[idx];
    } else {
      if (current[token] === undefined || current[token] === null) {
        current[token] = isNextArray ? [] : {};
      }
      current = current[token];
    }
  }

  const last = tokens[tokens.length - 1];
  if (last.startsWith('[') && last.endsWith(']')) {
    const idx = parseInt(last.slice(1, -1), 10);
    if (isArray(current)) {
      if (current[idx] !== undefined) {
        current[idx] = String(current[idx]) + String(value);
      } else {
        current[idx] = value;
      }
    }
  } else {
    if (current[last] !== undefined) {
      current[last] = String(current[last]) + String(value);
    } else {
      current[last] = value;
    }
  }

  return root;
};

// ─── GSJsonResult ─────────────────────────────────────────────────────────────

/**
 * ResultType enum — mirrors Kotlin GSJsonResult.ResultType
 */
const ResultType = Object.freeze({
  STRING:  'STRING',
  NUMBER:  'NUMBER',
  BOOLEAN: 'BOOLEAN',
  NULL:    'NULL',
  OBJECT:  'OBJECT',
  ARRAY:   'ARRAY',
});

/**
 * Build a GSJsonResult object — mirrors the Kotlin data class.
 */
const _createResult = (value) => {
  const exists = value !== undefined && value !== null;
  let type;
  if (!exists)                         type = ResultType.NULL;
  else if (isArray(value))             type = ResultType.ARRAY;
  else if (isObject(value))            type = ResultType.OBJECT;
  else if (typeof value === 'boolean') type = ResultType.BOOLEAN;
  else if (typeof value === 'number')  type = ResultType.NUMBER;
  else                                 type = ResultType.STRING;

  const raw = exists ? (typeof value === 'string' ? value : JSON.stringify(value)) : 'null';

  return {
    value,
    raw,
    type,
    exists,
    index: -1,
    string:  () => (exists ? String(value) : ''),
    int:     () => parseInt(value, 10) || 0,
    double:  () => parseFloat(value) || 0.0,
    float:   () => parseFloat(value) || 0.0,
    boolean: () => Boolean(value),
    bool:    () => Boolean(value),
    array:   () => (isArray(value) ? value.map((v) => _createResult(v)) : [_createResult(value)]),
    forEach: (fn) => {
      const arr = isArray(value) ? value.map((v) => _createResult(v)) : [_createResult(value)];
      arr.forEach(fn);
    },
  };
};

// ─── public API ──────────────────────────────────────────────────────────────

/**
 * Get a value from `data` using a GSJson path expression.
 *
 * @param {object|string} data          - The data object (or JSON string)
 * @param {string}        path          - GSJson path expression
 * @param {*}             [defaultValue]- Returned when the path resolves to null/undefined
 * @returns {*} The resolved value, or defaultValue
 */
const get = (data, path, defaultValue = undefined) => {
  if (data === null || data === undefined || !path) return defaultValue ?? undefined;

  let obj = data;
  if (typeof data === 'string') {
    // JSON Lines handled by _handleJsonLines — don't pre-parse
    if (!path.startsWith('..')) {
      try { obj = JSON.parse(data); } catch { return defaultValue ?? undefined; }
    }
  }

  const value = _getByPath(obj, path, obj, []);
  if (value === null || value === undefined) return defaultValue ?? undefined;
  return value;
};

/**
 * Set a value in `data` at the given GSJson path.
 * Mirrors GSJson.set() on the JVM side.
 *
 * @param {object|string} data   - The data object or JSON string (may be '' / '{}' for new)
 * @param {string}        path   - GSJson path expression
 * @param {*}             value  - Value to set
 * @returns {string} The resulting JSON string
 */
const set = (data, path, value) => {
  let obj;
  if (!data || data === '') {
    obj = {};
  } else if (typeof data === 'string') {
    try { obj = JSON.parse(data); } catch { obj = {}; }
  } else {
    obj = data;
  }

  const result = _setByPath(obj, path, value);
  return JSON.stringify(result);
};

/**
 * Check whether a path exists (resolves to a non-null/undefined value) in data.
 * Mirrors GSJson.exists() on the JVM side.
 */
const exists = (data, path) => {
  const v = get(data, path);
  return v !== undefined && v !== null;
};

/**
 * Rich result wrapper (mirrors GSJson.getResult on the JVM side).
 * Returns a GSJsonResult object with .value, .raw, .type, .exists,
 * .string(), .int(), .double(), .boolean(), .array(), .forEach().
 */
const getResult = (data, path) => {
  const value = get(data, path);
  return _createResult(value);
};

/**
 * Iterate through JSON Lines, calling action(GSJsonResult) for each line.
 * Return false from action to stop iteration.
 * Mirrors GSJson.forEachLine() on the JVM side.
 *
 * @param {string}   jsonLines - Newline-delimited JSON string
 * @param {function} action    - Called with GSJsonResult for each line; return false to stop
 */
const forEachLine = (jsonLines, action) => {
  if (typeof jsonLines !== 'string') return;
  const lines = jsonLines.trim().split('\n').map((l) => l.trim()).filter(Boolean);
  for (const line of lines) {
    try {
      const parsed = JSON.parse(line);
      const result = _createResult(parsed);
      if (action(result) === false) break;
    } catch { /* skip invalid */ }
  }
};

// ─── exports ──────────────────────────────────────────────────────────────────

const GSJson = { get, set, exists, getResult, forEachLine, ResultType };

export default GSJson;
export { get, set, exists, getResult, forEachLine, ResultType };
