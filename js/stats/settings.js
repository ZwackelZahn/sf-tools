const ScriptType = {
    History: 0,
    Players: 1,
    Group: 2,
    Tracker: 3
}

const EditorType = {
    DEFAULT: 0,
    ACTIONS: 1
}

const ARG_MAP = {
    'off': 0,
    'on': 1,
    'thick': 2,
    'thin': 1,
    'static': 2,
    'above or equal': 'ae',
    'below or equal': 'be',
    'equal or above': 'ae',
    'equal or below': 'be',
    'above': 'a',
    'below': 'b',
    'equal': 'e',
    'default': 'd',
    'none': 0,
    'left': 1,
    'right': 2,
    'both': 3,
    'top': 4,
    'bottom': 5
};

const ARG_MAP_SERVER = {
    'off': 0,
    'on': 100
}

const ARG_FORMATTERS = {
    'number': (p, c, e, x) => isNaN(x) ? undefined : (Number.isInteger(x) ? x : x.toFixed(2)),
    'fnumber': (p, c, e, x) => isNaN(x) ? undefined : formatAsSpacedNumber(x),
    'nnumber': (p, c, e, x) => isNaN(x) ? undefined : formatAsNamedNumber(x),
    'date': (p, c, e, x) => isNaN(x) ? undefined : formatDateOnly(x),
    'bool': (p, c, e, x) => x ? 'Yes' : 'No',
    'datetime': (p, c, e, x) => isNaN(x) ? undefined : formatDate(x),
    'time': (p, c, e, x) => isNaN(x) ? undefined : formatTime(x),
    'duration': (p, c, e, x) => isNaN(x) ? undefined : formatDuration(x),
    'default': (p, c, e, x) => typeof(x) == 'string' ? x : (isNaN(x) ? undefined : (Number.isInteger(x) ? x : x.toFixed(2)))
}

class CellStyle {
    constructor () {
        this.styles = {};
        this.content = '';
    }

    add (name, value) {
        let style = new Option().style;
        style[name] = value;

        if (style.cssText) {
            this.styles[name] = style.cssText.slice(0, -1) + ' !important';
        }

        this.content = Object.values(this.styles).join(';');
    }

    get cssText () {
        return this.content;
    }
}

class RuleEvaluator {
    constructor () {
        this.rules = [];
    }

    addRule (condition, referenceValue, value) {
        this.rules.push([ condition, referenceValue, value, isNaN(referenceValue) ? referenceValue : null ]);
    }

    get (value, ignoreBase = false) {
        for (let [ condition, referenceValue, output ] of this.rules) {
            if (condition == 'db') {
                if (ignoreBase) {
                    continue;
                } else {
                    return output;
                }
            } else if (condition == 'd') {
                return output;
            } else if (condition == 'e') {
                if (value == referenceValue) {
                    return output;
                }
            } else if (condition == 'a') {
                if (value > referenceValue) {
                    return output;
                }
            } else if (condition == 'b') {
                if (value < referenceValue) {
                    return output;
                }
            } else if (condition == 'ae') {
                if (value >= referenceValue) {
                    return output;
                }
            } else if (condition == 'be') {
                if (value <= referenceValue) {
                    return output;
                }
            }
        }

        return undefined;
    }
}

const FilterTypes = {
    'Guild': ScriptType.Group,
    'Player': ScriptType.History,
    'Players': ScriptType.Players
};

class Command {
    constructor (regexp, parse, format) {
        this.regexp = regexp;
        this.internalParse = parse;
        this.internalFormat = format;

        this.canParseAlways = false;
        this.canParse = true;
        this.canCopy = false;
        this.type = 0;
    }

    isValid (string) {
        return this.regexp.test(string);
    }

    parse (root, string) {
        return this.internalParse(root, ... string.match(this.regexp).slice(1));
    }

    parseAsMacro (string) {
        return string.match(this.regexp).slice(1);
    }

    format (root, string) {
        return this.internalFormat(root, ... string.match(this.regexp).slice(1));
    }

    parseAlways () {
        this.canParseAlways = true;
        return this;
    }

    parseNever () {
        this.canParse = false;
        return this;
    }

    copyable () {
        this.canCopy = true;
        return this;
    }

    specialType (type) {
        this.type = type;
        return this;
    }
}

const SettingsCommands = [
    /*
        If not
    */
    new Command(
        /^if not (.+)$/,
        null,
        (root, arg) => SFormat.Macro(`if not ${ arg }`)
    ).parseNever(),
    /*
        If
    */
    new Command(
        /^if (.+)$/,
        null,
        (root, arg) => SFormat.Macro(`if ${ arg }`)
    ).parseNever(),
    /*
        Else if
    */
    new Command(
        /^else if (.+)$/,
        null,
        (root, arg) => SFormat.Macro(`else if ${ arg }`)
    ).parseNever(),
    /*
        Else
    */
    new Command(
        /^else$/,
        null,
        (root) => SFormat.Macro('else')
    ).parseNever(),
    /*
        Loop
    */
    new Command(
        /^loop (\w+(?:\s*\,\s*\w+)*) for (.+)$/,
        null,
        (root, name, array) => SFormat.Macro(SFormat.Keyword('loop ') + SFormat.Constant(name) + SFormat.Keyword(' for ') + Expression.format(array, root), true)
    ).parseNever(),
    /*
        End loop or condition
    */
    new Command(
        /^end$/,
        null,
        (root) => SFormat.Macro('end')
    ).parseNever(),
    /*
        Macro-compatible function
    */
    new Command(
        /^mset (\w+[\w ]*) with (\w+[\w ]*(?:,\s*\w+[\w ]*)*) as (.+)$/,
        (root, name, arguments, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addFunction(name, ast, arguments.split(',').map(v => v.trim()));
            }
        },
        (root, name, arguments, expression) => SFormat.Macro('mset') + SFormat.Keyword(' ') + SFormat.Constant(name) + SFormat.Keyword(' with ') + arguments.split(',').map(v => SFormat.Constant(v)).join(',') + SFormat.Keyword(' as ') + Expression.format(expression, root),
    ).parseAlways(),
    /*
        Macro-compatible variable
    */
    new Command(
        /^mset (\w+[\w ]*) as (.+)$/,
        (root, name, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addVariable(name, ast, false);
            }
        },
        (root, name, expression) => SFormat.Macro('mset') + SFormat.Keyword(' ') + SFormat.Constant(name) + SFormat.Keyword(' as ') + Expression.format(expression, root),
    ).parseAlways(),
    /*
        Constant
    */
    new Command(
        /^const (\w+) (.+)$/,
        (root, name, value) => root.addConstant(name, value),
        (root, name, value) => SFormat.Keyword('const ') + SFormat.Constant(name) + ' ' + SFormat.Normal(value),
    ).parseAlways(),
    /*
        Constant Expression
    */
    new Command(
        /^constexpr (\w+) (.+)$/,
        (root, name, expression) => {
            let ast = new Expression(expression);
            if (ast.isValid()) {
                root.addConstant(name, new ExpressionScope(root).eval(ast));
            }
        },
        (root, name, expression) => SFormat.Keyword('constexpr ') + SFormat.Constant(name) + ' ' + Expression.format(expression, root),
    ).parseAlways(),
    /*
        Server column
    */
    new Command(
        /^server ((@?)(\S+))$/,
        (root, value, a, b) => {
            let val = root.constants.getValue(a, b);
            if (val != undefined) {
                if (isNaN(val)) {
                    val = ARG_MAP_SERVER[val];
                }

                if (!isNaN(val)) {
                    root.addGlobal('server', Number(val));
                }
            }
        },
        (root, value, a, b) => {
            let prefix = SFormat.Keyword('server ');
            let val = root.constants.getValue(a, b);

            if (ARG_MAP_SERVER.hasOwnProperty(value)) {
                return prefix + SFormat.Bool(value);
            } else if (root.constants.isValid(a, b) && !isNaN(val)) {
                return prefix + SFormat.Constant(value);
            } else if (a == '@' || isNaN(val)) {
                return prefix + SFormat.Error(value);
            } else {
                return prefix + SFormat.Normal(value);
            }
        }
    ),
    /*
        Name column
    */
    new Command(
        /^name ((@?)(\S+))$/,
        (root, value, a, b) => {
            let val = root.constants.getValue(a, b);
            if (val != undefined && !isNaN(val)) {
                root.addGlobal('name', Number(val));
            }
        },
        (root, value, a, b) => {
            let prefix = SFormat.Keyword('name ');
            let val = root.constants.getValue(a, b);

            if (root.constants.isValid(a, b) && !isNaN(val)) {
                return prefix + SFormat.Constant(value);
            } else if (a == '@' || isNaN(val)) {
                return prefix + SFormat.Error(value);
            } else {
                return prefix + SFormat.Normal(value);
            }
        }
    ),
    /*
        Column width
    */
    new Command(
        /^width ((@?)(\S+))$/,
        (root, value, a, b) => {
            let val = root.constants.getValue(a, b);
            if (val != undefined && !isNaN(val)) {
                root.addShared('width', Number(val));
            }
        },
        (root, value, a, b) => {
            let prefix = SFormat.Keyword('width ');
            let val = root.constants.getValue(a, b);

            if (root.constants.isValid(a, b) && !isNaN(val)) {
                return prefix + SFormat.Constant(value);
            } else if (a == '@' || isNaN(val)) {
                return prefix + SFormat.Error(value);
            } else {
                return prefix + SFormat.Normal(value);
            }
        }
    ).copyable(),
    /*
        Embed width configuration
    */
    new Command(
        /^columns (@?\w+[\w ]*(?:,\s*@?\w+[\w ]*)*)$/,
        (root, parts) => {
            let values = parts.split(',').map(p => root.constants.get(p.trim())).map(v => isNaN(v) ? 0 : parseInt(v));
            if (values.length > 0) {
                root.addLocal('columns', values);
            }
        },
        (root, parts) => {
            let prefix = SFormat.Keyword('columns ');
            let content = [];
            for (let part of parts.split(',')) {
                const value = root.constants.get(part.trim());
                if (isNaN(value)) {
                    content.push(SFormat.Error(part));
                } else if (part.trim()[0] == '@') {
                    content.push(SFormat.Constant(part));
                } else {
                    content.push(SFormat.Normal(part));
                }
            }

            return prefix + content.join(',');
        }
    ).copyable(),
    /*
        Not defined value
    */
    new Command(
        /^not defined value ((@?)(.+))$/,
        (root, value, a, b) => {
            let val = root.constants.getValue(a, b);
            if (val != undefined) {
                root.addShared('ndef', val);
            }
        },
        (root, value, a, b) => {
            let prefix = SFormat.Keyword('not defined value ');
            let val = root.constants.getValue(a, b);

            if (root.constants.isValid(a, b)) {
                return prefix + SFormat.Constant(value);
            } else if (a == '@') {
                return prefix + SFormat.Error(value);
            } else {
                return prefix + SFormat.Normal(value);
            }
        }
    ).copyable(),
    /*
        Not defined color
    */
    new Command(
        /^not defined color ((@?)(.+))$/,
        (root, value, a, b) => {
            let val = getCSSColor(root.constants.getValue(a, b));
            if (val != undefined && val) {
                root.addShared('ndefc', val);
            }
        },
        (root, value, a, b) => {
            let prefix = SFormat.Keyword('not defined color ');
            let val = getCSSColor(root.constants.getValue(a, b));

            if (root.constants.isValid(a, b) && val) {
                return prefix + SFormat.Constant(value);
            } else if (a == '@' || !val) {
                return prefix + SFormat.Error(value);
            } else {
                return prefix + SFormat.Color(value, val);
            }
        }
    ).copyable(),
    /*
        Value default rule
    */
    new Command(
        /^value default ((@?)(\S+[\S ]*))$/,
        (root, value, a, b) => {
            let val = root.constants.getValue(a, b);
            if (val != undefined) {
                root.addValueRule(ARG_MAP['default'], 0, val);
            }
        },
        (root, value, a, b) => {
            let prefix = SFormat.Keyword('value ') + SFormat.Constant('default ');
            if (root.constants.isValid(a, b)) {
                return prefix + SFormat.Constant(value);
            } else if (a == '@') {
                return prefix + SFormat.Error(value);
            } else {
                return prefix + SFormat.Normal(value);
            }
        }
    ).copyable(),
    /*
        Value rules
    */
    new Command(
        /^value (equal or above|above or equal|below or equal|equal or below|equal|above|below) ((@?)(.+)) ((@?)(\S+[\S ]*))$/,
        (root, rule, value, a, b, value2, a2, b2) => {
            let ref = root.constants.getValue(a, b);
            let val = root.constants.getValue(a2, b2);

            if (val != undefined && ref != undefined) {
                root.addValueRule(ARG_MAP[rule], ref, val);
            }
        },
        (root, rule, value, a, b, value2, a2, b2) => {
            let prefix = SFormat.Keyword('value ') + SFormat.Constant(rule) + ' ';

            if (root.constants.isValid(a, b)) {
                value = SFormat.Constant(value);
            } else if (a == '@') {
                value = SFormat.Error(value);
            } else {
                value = SFormat.Normal(value);
            }

            if (root.constants.isValid(a2, b2)) {
                value2 = SFormat.Constant(value2);
            } else if (a2 == '@') {
                value2 = SFormat.Error(value2);
            } else {
                value2 = SFormat.Normal(value2);
            }

            return prefix + value + ' ' + value2;
        }
    ).copyable(),
    /*
        Color default rule
    */
    new Command(
        /^color default ((@?)(\S+[\S ]*))$/,
        (root, value, a, b) => {
            let val = getCSSColor(root.constants.getValue(a, b));
            if (val != undefined && val) {
                root.addColorRule(ARG_MAP['default'], 0, val);
            }
        },
        (root, value, a, b) => {
            let prefix = SFormat.Keyword('color ') + SFormat.Constant('default ');
            let val = getCSSColor(root.constants.getValue(a, b));

            if (root.constants.isValid(a, b) && val) {
                return prefix + SFormat.Constant(value);
            } else if (a == '@' || !val) {
                return prefix + SFormat.Error(value);
            } else {
                return prefix + SFormat.Color(value, val);
            }
        }
    ).copyable(),
    /*
        Color rules
    */
    new Command(
        /^color (equal or above|above or equal|below or equal|equal or below|equal|above|below) ((@?)(.+)) ((@?)(\S+[\S ]*))$/,
        (root, rule, value, a, b, value2, a2, b2) => {
            let ref = root.constants.getValue(a, b);
            let val = getCSSColor(root.constants.getValue(a2, b2));

            if (val != undefined && ref != undefined && val) {
                root.addColorRule(ARG_MAP[rule], ref, val);
            }
        },
        (root, rule, value, a, b, value2, a2, b2) => {
            let prefix = SFormat.Keyword('color ') + SFormat.Constant(rule) + ' ';
            let val = getCSSColor(root.constants.getValue(a2, b2));

            if (root.constants.isValid(a, b)) {
                value = SFormat.Constant(value);
            } else if (a == '@') {
                value = SFormat.Error(value);
            } else {
                value = SFormat.Normal(value);
            }

            if (root.constants.isValid(a2, b2) && val) {
                value2 = SFormat.Constant(value2);
            } else if (a2 == '@' || !val) {
                value2 = SFormat.Error(value2);
            } else {
                value2 = SFormat.Color(value2, val);
            }

            return prefix + value + ' ' + value2;
        }
    ).copyable(),
    /*
        Alias
    */
    new Command(
        /^alias ((@?)(.+))$/,
        (root, value, a, b) => {
            let val = root.constants.getValue(a, b);
            if (val != undefined) {
                root.addAlias(val);
            }
        },
        (root, value, a, b) => {
            let prefix = SFormat.Keyword('alias ');
            let val = root.constants.getValue(a, b);

            if (root.constants.isValid(a, b)) {
                return prefix + SFormat.Constant(value);
            } else if (a == '@') {
                return prefix + SFormat.Error(value);
            } else {
                return prefix + SFormat.Normal(value);
            }
        }
    ).copyable(),
    /*
        Statistics format expression
    */
    new Command(
        /^format statistics (.+)$/,
        (root, expression) => {
            if (expression == 'on') {
                root.addFormatStatisticsExpression(true);
            } else if (expression == 'off') {
                root.addFormatStatisticsExpression(false);
            } else if (ARG_FORMATTERS.hasOwnProperty(expression)) {
                root.addFormatStatisticsExpression(ARG_FORMATTERS[expression])
            } else {
                let ast = new Expression(expression, root);
                if (ast.isValid()) {
                    root.addFormatStatisticsExpression(ast);
                }
            }
        },
        (root, expression) => SFormat.Keyword('format statistics ') + (expression == 'on' || expression == 'off' ? SFormat.Bool(expression) : (ARG_FORMATTERS.hasOwnProperty(expression) ? SFormat.Constant(expression) : Expression.format(expression, root)))
    ).copyable(),
    /*
        Difference format expression
    */
    new Command(
        /^format difference (.+)$/,
        (root, expression) => {
            if (expression == 'on') {
                root.addFormatDifferenceExpression(true);
            } else if (expression == 'off') {
                root.addFormatDifferenceExpression(false);
            } else if (ARG_FORMATTERS.hasOwnProperty(expression)) {
                root.addFormatDifferenceExpression(ARG_FORMATTERS[expression])
            } else {
                let ast = new Expression(expression, root);
                if (ast.isValid()) {
                    root.addFormatDifferenceExpression(ast);
                }
            }
        },
        (root, expression) => SFormat.Keyword('format difference ') + (expression == 'on' || expression == 'off' ? SFormat.Bool(expression) : (ARG_FORMATTERS.hasOwnProperty(expression) ? SFormat.Constant(expression) : Expression.format(expression, root)))
    ).copyable(),
    /*
        Cell background
    */
    new Command(
        /^background ((@?)(.+))$/,
        (root, value, a, b) => {
            let val = getCSSColor(root.constants.getValue(a, b));
            if (val != undefined && val) {
                root.addShared('background', val);
            }
        },
        (root, value, a, b) => {
            let prefix = SFormat.Keyword('background ');
            let val = getCSSColor(root.constants.getValue(a, b));

            if (root.constants.isValid(a, b) && val) {
                return prefix + SFormat.Constant(value);
            } else if (a == '@' || !val) {
                return prefix + SFormat.Error(value);
            } else {
                return prefix + SFormat.Color(value, val);
            }
        }
    ).copyable(),
    /*
        Format expression
    */
    new Command(
        /^(format|expf) (.+)$/,
        (root, token, expression) => {
            if (ARG_FORMATTERS.hasOwnProperty(expression)) {
                root.addFormatExpression(ARG_FORMATTERS[expression])
            } else {
                let ast = new Expression(expression, root);
                if (ast.isValid()) {
                    root.addFormatExpression(ast);
                }
            }
        },
        (root, token, expression) => SFormat.Keyword(`${ token } `) + (ARG_FORMATTERS.hasOwnProperty(expression) ? SFormat.Constant(expression) : Expression.format(expression, root))
    ).copyable(),
    /*
        Category
    */
    new Command(
        /^((?:\w+)(?:\,\w+)*:|)category(?: (.+))?$/,
        (root, extensions, name) => {
            root.addCategory(name || '', name == undefined);
            if (extensions) {
                root.addExtension(... extensions.slice(0, -1).split(','));
            }
        },
        (root, extensions, name) => (extensions ? SFormat.Constant(extensions) : '') + SFormat.Keyword('category') + (name ? (' ' + SFormat.Normal(name)) : '')
    ).copyable(),
    /*
        Grouped header
    */
    new Command(
        /^((?:\w+)(?:\,\w+)*:|)header(?: (.+))? as group of (\d+)$/,
        (root, extensions, name, length) => {
            if (length > 0) {
                root.addHeader(name || '', Number(length));
                if (extensions) {
                    root.addExtension(... extensions.slice(0, -1).split(','));
                }
            }
        },
        (root, extensions, name, length) => {
            let prefix = (extensions ? SFormat.Constant(extensions) : '') + SFormat.Keyword('header');
            let suffix = SFormat.Keyword(' as group of ') + SFormat.Constant(length);
            if (name != undefined) {
                if (SP_KEYWORD_MAPPING_0.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.Reserved(name) + suffix;
                } else if (SP_KEYWORD_MAPPING_1.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.ReservedProtected(name) + suffix;
                } else if (SP_KEYWORD_MAPPING_2.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.ReservedPrivate(name) + suffix;
                } else if (SP_KEYWORD_MAPPING_3.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.ReservedSpecial(name) + suffix;
                } else if (SP_KEYWORD_MAPPING_5_HO.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.ReservedItemizable(name) + suffix;
                } else {
                    return prefix + ' ' + SFormat.Normal(name) + suffix;
                }
            } else {
                return prefix + suffix;
            }
        }
    ).copyable(),
    /*
        Header
    */
    new Command(
        /^((?:\w+)(?:\,\w+)*:|)header(?: (.+))?$/,
        (root, extensions, name) => {
            root.addHeader(name || '');
            if (extensions) {
                root.addExtension(... extensions.slice(0, -1).split(','));
            }
        },
        (root, extensions, name) => {
            let prefix = (extensions ? SFormat.Constant(extensions) : '') + SFormat.Keyword('header');
            if (name != undefined) {
                if (SP_KEYWORD_MAPPING_0.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.Reserved(name);
                } else if (SP_KEYWORD_MAPPING_1.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.ReservedProtected(name);
                } else if (SP_KEYWORD_MAPPING_2.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.ReservedPrivate(name);
                } else if (SP_KEYWORD_MAPPING_3.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.ReservedSpecial(name);
                } else if (SP_KEYWORD_MAPPING_5_HO.hasOwnProperty(name)) {
                    return prefix + ' ' + SFormat.ReservedItemizable(name);
                } else {
                    return prefix + ' ' + SFormat.Normal(name);
                }
            } else {
                return prefix;
            }
        }
    ).copyable(),
    /*
        Row
    */
    new Command(
        /^((?:\w+)(?:\,\w+)*:|)show (\S+[\S ]*) as (\S+[\S ]*)$/,
        (root, extensions, name, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addRow(name, ast);
                if (extensions) {
                    root.addExtension(... extensions.slice(0, -1).split(','));
                }
            }
        },
        (root, extensions, name, expression) => (extensions ? SFormat.Constant(extensions) : '') + SFormat.Keyword('show ') + SFormat.Constant(name) + SFormat.Keyword(' as ') + Expression.format(expression, root)
    ),
    /*
        Var
    */
    new Command(
        /^var (\w+) (.+)$/,
        (root, name, value) => root.addHeaderVariable(name, value),
        (root, name, value) => SFormat.Keyword('var ') + SFormat.Constant(name) + ' ' + SFormat.Normal(value)
    ).copyable(),
    /*
        Embedded table end
    */
    new Command(
        /^embed end$/,
        (root) => root.pushEmbed(),
        (root) => SFormat.Keyword('embed end')
    ).copyable(),
    /*
        Embedded table
    */
    new Command(
        /^((?:\w+)(?:\,\w+)*:|)embed(?: (.+))?$/,
        (root, extensions, name) => {
            root.embedBlock(name || '');
            if (extensions) {
                root.addExtension(... extensions.slice(0, -1).split(','));
            }
        },
        (root, extensions, name) => (extensions ? SFormat.Constant(extensions) : '') + SFormat.Keyword('embed') + (name ? (' ' + SFormat.Normal(name)) : '')
    ).copyable(),
    /*
        Layout
    */
    new Command(
        /^layout ((\||\_|table|statistics|rows|members)(\s+(\||\_|table|statistics|rows|members))*)$/,
        (root, layout) => root.addLayout(layout.split(/\s+/).map(v => v.trim())),
        (root, layout) => SFormat.Keyword('layout ') + SFormat.Constant(layout)
    ),
    /*
        Table variable
    */
    new Command(
        /^set (\w+[\w ]*) with all as (.+)$/,
        (root, name, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addVariable(name, ast, true);
            }
        },
        (root, name, expression) => SFormat.Keyword('set ') + SFormat.Global(name) + SFormat.Keyword(' with all as ') + Expression.format(expression, root),
    ).parseAlways(),
    /*
        New syntax for table variable
    */
    new Command(
        /^set \$(\w+[\w ]*) as (.+)$/,
        (root, name, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addVariable(name, ast, true);
            }
        },
        (root, name, expression) => SFormat.Keyword('set ') + SFormat.Global(`$${name}`) + SFormat.Keyword(' as ') + Expression.format(expression, root)
    ).parseAlways(),
    /*
        New syntax for unfiltered table variable
    */
    new Command(
        /^set \$\$(\w+[\w ]*) as (.+)$/,
        (root, name, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addVariable(name, ast, 'unfiltered');
            }
        },
        (root, name, expression) => SFormat.Keyword('set ') + SFormat.UnfilteredGlobal(`$$${name}`) + SFormat.Keyword(' as ') + Expression.format(expression, root)
    ).parseAlways(),
    /*
        Function
    */
    new Command(
        /^set (\w+[\w ]*) with (\w+[\w ]*(?:,\s*\w+[\w ]*)*) as (.+)$/,
        (root, name, arguments, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addFunction(name, ast, arguments.split(',').map(v => v.trim()));
            }
        },
        (root, name, arguments, expression) => SFormat.Keyword('set ') + SFormat.Constant(name) + SFormat.Keyword(' with ') + arguments.split(',').map(v => SFormat.Constant(v)).join(',') + SFormat.Keyword(' as ') + Expression.format(expression, root),
    ).parseAlways(),
    /*
        Variable
    */
    new Command(
        /^set (\w+[\w ]*) as (.+)$/,
        (root, name, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addVariable(name, ast, false);
            }
        },
        (root, name, expression) => SFormat.Keyword('set ') + SFormat.Constant(name) + SFormat.Keyword(' as ') + Expression.format(expression, root),
    ).parseAlways(),
    /*
        Lined
    */
    new Command(
        /^lined$/,
        (root, value) => root.addGlobal('lined', 1),
        (root, value) => SFormat.Keyword('lined')
    ),
    new Command(
        /^lined (on|off|thin|thick)$/,
        (root, value) => root.addGlobal('lined', ARG_MAP[value]),
        (root, value) => SFormat.Keyword('lined ') + SFormat.Bool(value, value == 'thick' || value == 'thin' ? 'on' : value)
    ),
    /*
        Performance (entry cutoff)
    */
    new Command(
        /^performance (\d+)$/,
        (root, value) => {
            if (value > 0) {
                root.addGlobal('performance', Number(value));
            }
        },
        (root, value) => SFormat.Keyword('performance ') + (value > 0 ? SFormat.Normal(value) : SFormat.Error(value))
    ),
    /*
        Scale
    */
    new Command(
        /^scale (\d+)$/,
        (root, value) => {
            if (value > 0) {
                root.addGlobal('scale', Number(value));
            }
        },
        (root, value) => SFormat.Keyword('scale ') + (value > 0 ? SFormat.Normal(value) : SFormat.Error(value))
    ),
    /*
        Row height
    */
    new Command(
        /^row height (\d+)$/,
        (root, value) => {
            if (value > 0) {
                root.addGlobalEmbedable('row_height', Number(value));
            }
        },
        (root, value) => SFormat.Keyword('row height ') + (value > 0 ? SFormat.Normal(value) : SFormat.Error(value))
    ),
    /*
        Font
    */
    new Command(
        /^font (.+)$/,
        (root, font) => {
            let value = getCSSFont(font);
            if (value) {
                root.addGlobalEmbedable('font', value);
            }
        },
        (root, font) => SFormat.Keyword('font ') + (getCSSFont(font) ? SFormat.Normal(font) : SFormat.Error(font))
    ),
    /*
        Shared options
    */
    new Command(
        /^(difference|hydra|flip|brackets|statistics|maximum|grail|decimal) (on|off)$/,
        (root, key, value) => root.addShared(key, ARG_MAP[value]),
        (root, key, value) => SFormat.Keyword(key) + ' ' + SFormat.Bool(value)
    ).copyable(),
    /*
        Clean
    */
    new Command(
        /^clean$/,
        (root) => root.addLocal('clean', 1),
        (root) => SFormat.Keyword('clean')
    ).copyable(),
    new Command(
        /^clean hard$/,
        (root) => root.addLocal('clean', 2),
        (root) => SFormat.Keyword('clean ') + SFormat.Constant('hard')
    ).copyable(),
    /*
        Action
    */
    new Command(
        /^action (none|show)$/,
        (root, value) => root.addAction(value),
        (root, value) => SFormat.Keyword('action ') + SFormat.Constant(value)
    ).copyable(),
    /*
        Indexing
    */
    new Command(
        /^indexed$/,
        (root, value) => root.addGlobal('indexed', 1),
        (root, value) => SFormat.Keyword('indexed')
    ),
    new Command(
        /^indexed (on|off|static)$/,
        (root, value) => root.addGlobal('indexed', ARG_MAP[value]),
        (root, value) => SFormat.Keyword('indexed ') + SFormat.Bool(value, value == 'static' ? 'on' : value)
    ),
    /*
        Global options
    */
    new Command(
        /^(members|outdated|opaque|large rows|align title)$/,
        (root, key) => root.addGlobal(key, true),
        (root, key) => SFormat.Keyword(key)
    ),
    new Command(
        /^(members|outdated|opaque|large rows|align title) (on|off)$/,
        (root, key, value) => root.addGlobal(key, ARG_MAP[value]),
        (root, key, value) => SFormat.Keyword(key) + ' ' + SFormat.Bool(value)
    ),
    /*
        Custom left category
    */
    new Command(
        /^((?:\w+)(?:\,\w+)*:|)left category$/,
        (root, extensions) => {
            root.addGlobal('custom left', true);
            root.addCategory('', true);
            if (extensions) {
                root.addExtension(... extensions.slice(0, -1).split(','));
            }
        },
        (root, extensions) => (extensions ? SFormat.Constant(extensions) : '') + SFormat.Keyword('left category')
    ),
    /*
        Simulator target
    */
    new Command(
        /^simulator (target|source) (\S+)$/,
        (root, mode, identifier) => {
            root.addGlobal('simulator_target', identifier);
            root.addGlobal('simulator_target_source', mode == 'source');
        },
        (root, mode, identifier) => SFormat.Keyword('simulator ') + SFormat.Keyword(mode) + ' ' + (identifier in DatabaseManager.Players ? SFormat.Constant(identifier) : SFormat.Error(identifier))
    ),
    /*
        Statistics
    */
    new Command(
        /^statistics (\S+[\S ]*) as (\S+[\S ]*)$/,
        (root, name, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addStatistics(name, ast);
            }
        },
        (root, name, expression) => SFormat.Keyword('statistics ') + SFormat.Constant(name) + SFormat.Keyword(' as ') + Expression.format(expression, root)
    ),
    /*
        Simulator cycles
    */
    new Command(
        /^simulator (\d+)$/,
        (root, value) => {
            if (value > 0) {
                root.addGlobal('simulator', Number(value));
            }
        },
        (root, value) => SFormat.Keyword('simulator ') + (value > 0 ? SFormat.Normal(value) : SFormat.Error(value))
    ),
    /*
        Extra expression
    */
    new Command(
        /^extra (.+)$/,
        (root, value) => root.addFormatExtraExpression(a => value),
        (root, value) => SFormat.Keyword('extra ') + SFormat.Normal(value)
    ).copyable(),
    /*
        Cell style
    */
    new Command(
        /^style ([a-zA-Z\-]+) (.*)$/,
        (root, style, value) => root.addStyle(style, value),
        (root, style, value) => SFormat.Keyword('style ') + SFormat.Constant(style) + ' ' + SFormat.Normal(value)
    ).copyable(),
    /*
        Cell content visibility
    */
    new Command(
        /^visible (on|off)$/,
        (root, value) => root.addShared('visible', ARG_MAP[value]),
        (root, value) => SFormat.Keyword('visible ') + SFormat.Bool(value)
    ).copyable(),
    /*
        Cell content breaking
    */
    new Command(
        /^breakline (on|off)$/,
        (root, value) => root.addBreaklineRule(ARG_MAP[value]),
        (root, value) => SFormat.Keyword('breakline ') + SFormat.Bool(value)
    ).copyable(),
    /*
        Cell border
    */
    new Command(
        /^border (none|left|right|both|top|bottom)$/,
        (root, value) => root.addShared('border', ARG_MAP[value]),
        (root, value) => SFormat.Keyword('border ') + SFormat.Constant(value)
    ).copyable(),
    /*
        Toggle statistics color
    */
    new Command(
        /^statistics color (on|off)$/,
        (root, value) => root.addShared('statistics_color', ARG_MAP[value]),
        (root, value) => SFormat.Keyword('statistics color ') + SFormat.Bool(value)
    ).copyable(),
    /*
        Order expression
    */
    new Command(
        /^order by (.+)$/,
        (root, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addLocal('order', ast);
            }
        },
        (root, expression) => SFormat.Keyword('order by ') + Expression.format(expression, root)
    ).copyable(),
    new Command(
        /^glob order (asc|des)(?: (\d+))?$/,
        (root, value, index) => root.addGlobOrder(parseInt(index), value == 'asc'),
        (root, value, index) => SFormat.Keyword('glob order ') + SFormat.Constant(`${ value } ${ index || '' }`)
    ).copyable(),
    /*
        Value expression
    */
    new Command(
        /^expr (.+)$/,
        (root, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addLocal('expr', ast);
            }
        },
        (root, expression) => SFormat.Keyword('expr ') + Expression.format(expression, root)
    ).copyable(),
    /*
        Alias expression
    */
    new Command(
        /^expa (.+)$/,
        (root, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addAliasExpression((a, b) => new ExpressionScope(a).eval(ast, b));
            }
        },
        (root, expression) => SFormat.Keyword('expa ') + Expression.format(expression, root)
    ).copyable(),
    /*
        Cell alignment
    */
    new Command(
        /^align (left|right|center)$/,
        (root, value) => root.addShared('align', value),
        (root, value) => SFormat.Keyword('align ') + SFormat.Constant(value)
    ).copyable(),
    new Command(
        /^align (left|right|center) (left|right|center)$/,
        (root, value, value2) => {
            root.addShared('align', value);
            root.addShared('align_title', value2);
        },
        (root, value, value2) => SFormat.Keyword('align ') + SFormat.Constant(value) + ' ' + SFormat.Constant(value2)
    ).copyable(),
    /*
        Discard expression
    */
    new Command(
        /^discard (.+)$/,
        (root, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addDiscardRule(ast);
            }
        },
        (root, expression) => SFormat.Keyword('discard ') + Expression.format(expression, root)
    ),
    /*
        Order all expression
    */
    new Command(
        /^order all by (.+)$/,
        (root, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addDefaultOrder(ast);
            }
        },
        (root, expression) => SFormat.Keyword('order all by ') + Expression.format(expression, root)
    ),
    /*
        Color expression
    */
    new Command(
        /^expc (.+)$/,
        (root, expression) => {
            let ast = new Expression(expression, root);
            if (ast.isValid()) {
                root.addColorExpression(ast);
            }
        },
        (root, expression) => SFormat.Keyword('expc ') + Expression.format(expression, root)
    ).copyable(),
    /*
        Cell padding (left only)
    */
    new Command(
        /^padding (.+)$/,
        (root, value) => root.addLocal('padding', value),
        (root, value) => SFormat.Keyword('padding ') + SFormat.Normal(value)
    ).copyable(),
    /*
        Define extension
    */
    new Command(
        /^define (\w+)$/,
        (root, name) => root.addDefinition(name),
        (root, name) => SFormat.Keyword('define ') + SFormat.Normal(name),
        true
    ),
    /*
        Apply extension
    */
    new Command(
        /^extend (\w+)$/,
        (root, name) => root.addExtension(name),
        (root, name) => SFormat.Keyword('extend ') + SFormat.Constant(name)
    ),
    /*
        Force push current header / row / statistic
    */
    new Command(
        /^push$/,
        (root) => root.push(),
        (root) => SFormat.Keyword('push')
    ).parseAlways(),
    /*
        Tag action
    */
    new Command(
        /^tag (player|file) as (.+) if (.+)$/,
        (root, type, tag, expr) => {
            let ast1 = new Expression(tag);
            let ast2 = new Expression(expr);
            if (ast1.isValid() && ast2.isValid()) {
                root.addActionEntry('tag', type, ast1, ast2);
            }
        },
        (root, type, tag, expr) => SFormat.Keyword('tag ') + SFormat.Constant(type) + SFormat.Keyword(' as ') + Expression.format(tag, undefined, ACTION_PROPS) + SFormat.Keyword(' if ') + Expression.format(expr, undefined, ACTION_PROPS)
    ).specialType(EditorType.ACTIONS),
    new Command(
        /^remove player if (.+)$/,
        (root, expr) => {
            let ast1 = new Expression(expr);
            if (ast1.isValid()) {
                root.addActionEntry('remove', 'player', ast1);
            }
        },
        (root, expr) => SFormat.Keyword('remove ') + SFormat.Constant('player') + SFormat.Keyword(' if ') + Expression.format(expr, undefined, ACTION_PROPS)
    ).specialType(EditorType.ACTIONS),
    /*
        Tracker
    */
    new Command(
        /^(track (\w+(?:[ \w]*\w)?) as (.+) when (.+))$/,
        (root, str, name, arg, arg2) => {
            let ast = new Expression(arg);
            let ast2 = new Expression(arg2);
            if (ast.isValid() && ast2.isValid()) {
                root.addTracker(name, str, ast2, ast);
            }
        },
        (root, str, name, arg, arg2) => SFormat.Keyword('track ') + SFormat.Constant(name) + SFormat.Keyword(' as ') + Expression.format(arg) + SFormat.Keyword(' when ') + Expression.format(arg2)
    ),
    new Command(
        /^(track (\w+(?:[ \w]*\w)?) when (.+))$/,
        (root, str, name, arg) => {
            let ast = new Expression(arg);
            if (ast.isValid()) {
                root.addTracker(name, str, ast);
            }
        },
        (root, str, name, arg) => SFormat.Keyword('track ') + SFormat.Constant(name) + SFormat.Keyword(' when ') + Expression.format(arg)
    )
];

SettingsCommands.MACRO_IFNOT = SettingsCommands[0];
SettingsCommands.MACRO_IF = SettingsCommands[1];
SettingsCommands.MACRO_ELSEIF = SettingsCommands[2];
SettingsCommands.MACRO_ELSE = SettingsCommands[3];
SettingsCommands.MACRO_LOOP = SettingsCommands[4];
SettingsCommands.MACRO_END = SettingsCommands[5];
SettingsCommands.MACRO_FUNCTION = SettingsCommands[6];
SettingsCommands.MACRO_VARIABLE = SettingsCommands[7];
SettingsCommands.MACRO_CONST = SettingsCommands[8];
SettingsCommands.MACRO_CONSTEXPR = SettingsCommands[9];

class Settings {
    // Contructor
    constructor (string, type, scriptType = 0) {
        this.code = string;
        this.type = type;
        this.env_id = RandomSHA();

        // Constants
        this.constants = new Constants();

        // Discard rules
        this.discardRules = [];

        // Variables and functions
        this.functions = {};
        this.variables = {};
        this.variablesReference = {};

        this.trackers = {};

        // Lists
        this.lists = [];
        this.row_indexes = {};

        // Table
        this.categories = [];
        this.customStatistics = [];
        this.customRows = [];

        // Other things
        this.customDefinitions = {};
        this.actions = [];

        // Settings
        this.globals = {};

        // Shared globals
        this.shared = {
            statistics_color: true,
            visible: true
        };

        // Shared category
        this.sharedCategory = null;

        // Temporary objects
        this.category = null;
        this.header = null;
        this.definition = null;
        this.row = null;
        this.embed = null;

        // Parse settings
        for (let line of Settings.handleMacros(string, type)) {
            let command = SettingsCommands.find(command => command.isValid(line));
            if (command && command.canParse && command.type == scriptType) {
                command.parse(this, line);
            }
        }

        // Push last embed && category
        this.pushEmbed();
        this.pushCategory();
    }

    static handleConditionals (lines, type, scope) {
        let output = [];

        let condition = false;
        let shouldDiscard = false;

        for (let i = 0; i < lines.length; i++) {
            let line = lines[i];

            if (SettingsCommands.MACRO_IF.isValid(line)) {
                let rule = null;
                let ruleMustBeTrue = false;

                if (SettingsCommands.MACRO_IFNOT.isValid(line)) {
                    rule = SettingsCommands.MACRO_IFNOT;
                    ruleMustBeTrue = true;
                } else {
                    rule = SettingsCommands.MACRO_IF;
                }

                let cond = rule.parseAsMacro(line)[0].trim();
                if (cond in FilterTypes) {
                    shouldDiscard = ruleMustBeTrue ? (FilterTypes[cond] == type) : (FilterTypes[cond] != type);
                    condition = true;
                } else {
                    let condExpression = new Expression(cond);
                    if (condExpression.isValid()) {
                        let result = scope.eval(condExpression);
                        shouldDiscard = ruleMustBeTrue ? result : !result;
                        condition = true;
                    }
                }
            } else if (SettingsCommands.MACRO_ELSEIF.isValid(line)) {
                if (condition) {
                    if (shouldDiscard) {
                        let cond = SettingsCommands.MACRO_ELSEIF.parseAsMacro(line)[0].trim();
                        if (cond in FilterTypes) {
                            shouldDiscard = FilterTypes[cond] != type;
                        } else {
                            let condExpression = new Expression(cond);
                            if (condExpression.isValid()) {
                                let result = scope.eval(condExpression);
                                shouldDiscard = !result;
                            }
                        }
                    } else {
                        shouldDiscard = true;
                    }
                }
            } else if (SettingsCommands.MACRO_ELSE.isValid(line)) {
                if (condition) {
                    shouldDiscard = !shouldDiscard;
                }
            } else if (SettingsCommands.MACRO_LOOP.isValid(line)) {
                let endsRequired = 1;
                if (!shouldDiscard) {
                    output.push(line);
                }

                while (++i < lines.length) {
                    line = lines[i];

                    if (SettingsCommands.MACRO_IF.isValid(line) || SettingsCommands.MACRO_LOOP.isValid(line)) {
                        endsRequired++;
                        if (!shouldDiscard) {
                            output.push(line);
                        }
                    } else if (SettingsCommands.MACRO_END.isValid(line)) {
                        if (!shouldDiscard) {
                            output.push(line);
                        }

                        if (--endsRequired == 0) break;
                    } else if (!shouldDiscard) {
                        output.push(line);
                    }
                }
            } else if (SettingsCommands.MACRO_END.isValid(line)) {
                shouldDiscard = false;
                condition = false;
            } else if (!shouldDiscard) {
                output.push(line);
            }
        }

        return output;
    }

    static handleLoops (lines, scope) {
        let output = [];

        for (let i = 0; i < lines.length; i++) {
            let line = lines[i];

            if (SettingsCommands.MACRO_LOOP.isValid(line)) {
                let [ names, values ] = SettingsCommands.MACRO_LOOP.parseAsMacro(line);

                let variableNames = names.split(',').map(name => name.trim());
                let variableValues = [];

                let valuesExpression = new Expression(values);
                if (valuesExpression.isValid()) {
                    variableValues = scope.eval(valuesExpression);
                    if (!variableValues) {
                        variableValues = [];
                    } else if (!Array.isArray(variableValues)) {
                        if (typeof variableValues == 'object') {
                            variableValues = Object.values(variableValues);
                        } else {
                            variableValues = [ variableValues ];
                        }
                    }
                }

                let loop = [];

                let endsRequired = 1;
                while (++i < lines.length) {
                    line = lines[i];

                    if (SettingsCommands.MACRO_END.isValid(line)) {
                        if (--endsRequired == 0) break;
                    } else if (SettingsCommands.MACRO_IF.isValid(line) || SettingsCommands.MACRO_LOOP.isValid(line)) {
                        endsRequired++;
                    }

                    loop.push(line);
                }

                if (endsRequired != 0) {
                    output.push(... loop);
                } else {
                    for (let block of variableValues) {
                        if (!Array.isArray(block)) {
                            block = [ block ];
                        }

                        let varArray = variableNames.map((key, index) => `var ${ key } ${ block[index] }`);
                        let replacementArray = variableNames.map((key, index) => {
                            return {
                                regexp: new RegExp(`__${ key }__`, 'g'),
                                value: block[index]
                            }
                        });

                        for (let loopLine of loop) {
                            for (let { regexp, value } of replacementArray) {
                                loopLine = loopLine.replace(regexp, value)
                            }

                            output.push(loopLine);

                            if (/^(?:\w+(?:\,\w+)*:|)(?:header|embed|show|category)(?: .+)?$/.test(loopLine)) {
                                output.push(... varArray);
                            }
                        }
                    }
                }
            } else {
                output.push(line);
            }
        }

        return output;
    }

    static handleMacroVariables (lines, constants) {
        let settings = {
            functions: { },
            variables: { },
            constants: constants,
            lists: { }
        };

        let is_unsafe = 0;
        for (let line of lines) {
            if (SettingsCommands.MACRO_IF.isValid(line) || SettingsCommands.MACRO_LOOP.isValid(line)) {
                is_unsafe++;
            } else if (SettingsCommands.MACRO_END.isValid(line)) {
                if (is_unsafe > 0) {
                    is_unsafe--;
                }
            } else if (is_unsafe == 0) {
                if (SettingsCommands.MACRO_FUNCTION.isValid(line)) {
                    let [name, variables, expression] = SettingsCommands.MACRO_FUNCTION.parseAsMacro(line);
                    let ast = new Expression(expression);
                    if (ast.isValid()) {
                        settings.functions[name] = {
                            ast: ast,
                            args: variables.split(',').map(v => v.trim())
                        };
                    }
                } else if (SettingsCommands.MACRO_VARIABLE.isValid(line)) {
                    let [name, expression] = SettingsCommands.MACRO_VARIABLE.parseAsMacro(line);
                    let ast = new Expression(expression);
                    if (ast.isValid()) {
                        settings.variables[name] = {
                            ast: ast,
                            tableVariable: false
                        };
                    }
                } else if (SettingsCommands.MACRO_CONST.isValid(line)) {
                    let [name, value] = SettingsCommands.MACRO_CONST.parseAsMacro(line);
                    settings.constants.addConstant(name, value);
                } else if (SettingsCommands.MACRO_CONSTEXPR.isValid(line)) {
                    let [name, expression] = SettingsCommands.MACRO_CONSTEXPR.parseAsMacro(line);

                    let ast = new Expression(expression);
                    if (ast.isValid()) {
                        settings.constants.addConstant(name, new ExpressionScope(settings).eval(ast));
                    }
                }
            }
        }

        return settings;
    }

    static handleMacros (string, type) {
        let lines = string.split('\n').map(line => Settings.stripComments(line)[0].trim()).filter(line => line.length);

        // Scope for macros
        let scope = new ExpressionScope().add({
            table: type,
        }).add(SiteOptions.options);

        // Special constants for macros
        let constants = new Constants();
        constants.addConstant('guild', ScriptType.Group);
        constants.addConstant('player', ScriptType.History);
        constants.addConstant('players', ScriptType.Players);

        // Generate initial settings
        let settings = Settings.handleMacroVariables(lines, constants);
        while (lines.some(line => SettingsCommands.MACRO_IF.isValid(line) || SettingsCommands.MACRO_LOOP.isValid(line))) {
            lines = Settings.handleConditionals(lines, type, scope.environment(settings));
            settings = Settings.handleMacroVariables(lines, constants);
            lines = Settings.handleLoops(lines, scope.environment(settings));
            settings = Settings.handleMacroVariables(lines, constants);
        }

        return lines;
    }

    // Merge definition to object
    mergeDefinition (obj, name) {
        let definition = this.customDefinitions[name];
        if (definition) {
            // Merge commons
            for (var [ key, value ] of Object.entries(definition)) {
                if (!obj.hasOwnProperty(key)) obj[key] = definition[key];
            }

            // Merge color expression
            if (!obj.color.expr) {
                obj.color.expr = definition.color.expr;
            }

            // Merge color rules
            if (!obj.color.rules.rules.length) {
                obj.color.rules.rules = definition.color.rules.rules;
            }

            // Merge value expression
            if (!obj.value.format) {
                obj.value.format = definition.value.format;
            }

            // Merge difference format expression
            if (!obj.value.format) {
                obj.value.formatDifference = definition.value.formatDifference;
            }

            // Merge statistics format expression
            if (!obj.value.format) {
                obj.value.formatStatistics = definition.value.formatStatistics;
            }

            // Merge value extra
            if (!obj.value.extra) {
                obj.value.extra = definition.value.extra;
            }

            // Merge value rules
            if (!obj.value.rules.rules.length) {
                obj.value.rules.rules = definition.value.rules.rules;
            }

            this.mergeStyles(obj, definition.style);
            this.mergeVariables(obj, definition.vars);
        }
    }

    addTracker (name, str, ast, out) {
        this.trackers[name] = {
            str: str,
            ast: ast,
            out: out,
            hash: ast.rstr + (out ? out.rstr : '0000000000000000')
        };
    }

    addActionEntry (action, type, ...args) {
        this.actions.push({
            action,
            type,
            args
        });
    }

    // Merge mapping to object
    mergeMapping (obj, mapping) {
        // Merge commons
        for (var [ key, value ] of Object.entries(mapping)) {
            if (!obj.hasOwnProperty(key)) obj[key] = mapping[key];
        }

        // Merge value expression
        if (!obj.value.format) {
            obj.value.format = mapping.format;
        }

        // Merge diff value expression
        if (!obj.value.formatDifference) {
            obj.value.formatDifference = mapping.format_diff;
        }

        // Merge value extra
        if (!obj.value.extra) {
            obj.value.extra = mapping.extra;
        }

        this.mergeStyles(obj, mapping.style);
        this.mergeVariables(obj, mapping.vars);
    }

    merge (obj, mapping) {
        // Merge all non-objects
        for (var [ key, value ] of Object.entries(mapping)) {
            if (!obj.hasOwnProperty(key) && typeof value != 'object') obj[key] = mapping[key];
        }

        this.mergeStyles(obj, mapping.style);
        this.mergeVariables(obj, mapping.vars);
    }

    mergeStyles (obj, sourceStyle) {
        if (sourceStyle) {
            if (obj.style) {
                // Rewrite styles
                for (let [ name, value ] of Object.entries(sourceStyle.styles)) {
                    if (!obj.style.styles.hasOwnProperty(name)) {
                        obj.style.add(name, value);
                    }
                }
            } else {
                // Add whole style class
                obj.style = sourceStyle;
            }
        }
    }

    mergeVariables (obj, sourceVars) {
        if (sourceVars) {
            if (obj.vars) {
                // Add vars
                for (let [ name, value ] of Object.entries(sourceVars)) {
                    if (!obj.vars.hasOwnProperty(name)) {
                        obj.vars[name] = value;
                    }
                }
            } else {
                // Add whole list
                obj.vars = sourceVars;
            }
        }
    }

    // Push all settings
    push () {
        let obj = null;

        // Push definition
        obj = this.definition;
        if (obj) {
            this.customDefinitions[obj.name] = obj;
            this.definition = null;
        }

        // Push row
        obj = this.row;
        if (obj) {
            // Merge definitions
            for (let definitionName of obj.extensions || []) {
                this.mergeDefinition(obj, definitionName);
            }

            // Merge shared
            this.merge(obj, this.shared);

            // Push
            this.customRows.push(obj);
            this.row = null;
        }

        // Push header
        obj = this.header;
        if (obj && (this.embed || this.category)) {
            let name = obj.name;

            // Get mapping if exists
            let mapping = SP_KEYWORD_MAPPING_0[name] || SP_KEYWORD_MAPPING_1[name] || SP_KEYWORD_MAPPING_2[name] || SP_KEYWORD_MAPPING_3[name] || SP_KEYWORD_MAPPING_5_HO[name];

            // Merge definitions
            for (let definitionName of obj.extensions || []) {
                this.mergeDefinition(obj, definitionName);
            }

            // Add specials
            let custom = SP_SPECIAL_CONDITIONS[name];
            if (custom && obj.clean != 2) {
                for (let entry of custom) {
                    if (entry.condition(obj)) {
                        entry.apply(obj);
                    }
                }
            }

            // Add mapping or expression
            if (mapping && !obj.expr) {
                if (obj.clean == 2) {
                    obj.expr = mapping.expr;
                } else {
                    this.mergeMapping(obj, mapping);
                }
            }

            // Push header if possible
            if (obj.expr) {
                if (!obj.clean) {
                    if (this.category) {
                        this.merge(obj, this.sharedCategory);
                    }

                    this.merge(obj, this.shared);
                } else {
                    this.merge(obj, {
                        visible: true,
                        statistics_color: true
                    });
                }

                if (obj.background) {
                    obj.color.rules.addRule('db', 0, obj.background);
                }

                // Push
                (this.embed || this.category).headers.push(obj);
            }

            this.header = null;
        }
    }

    // Push category
    pushCategory () {
        this.push();

        // Push category
        let obj = this.category;
        if (obj) {
            this.merge(obj, this.sharedCategory);

            this.categories.push(obj);
            this.category = null;
        }
    }

    // Create color block
    getColorBlock () {
        return {
            expr: undefined,
            rules: new RuleEvaluator(),
            get: function (player, compare, settings, value, extra = undefined, ignoreBase = false, header = undefined, alternateSelf = undefined) {
                // Get color from expression
                let expressionColor = this.expr ? new ExpressionScope(settings).with(player, compare).addSelf(alternateSelf).addSelf(value).add(extra).via(header).eval(this.expr) : undefined;

                // Get color from color block
                let blockColor = this.rules.get(value, ignoreBase || (typeof expressionColor !== 'undefined'));

                // Return color or empty string
                return (typeof blockColor === 'undefined' ? getCSSBackground(expressionColor) : blockColor) || '';
            }
        }
    }

    // Create value block
    getValueBlock () {
        return {
            extra: undefined,
            format: undefined,
            breakline: true,
            formatDifference: undefined,
            formatStatistics: undefined,
            rules: new RuleEvaluator(),
            get: function (player, compare, settings, value, extra = undefined, header = undefined, alternateSelf = undefined) {
                // Get value from value block
                let output = this.rules.get(value);

                // Get value from format expression
                if (typeof output == 'undefined') {
                    if (this.format instanceof Expression) {
                        output = new ExpressionScope(settings).with(player, compare).addSelf(alternateSelf).addSelf(value).add(extra).via(header).eval(this.format);
                    } else if (typeof this.format == 'function') {
                        output = this.format(player, compare, settings, value, extra, header);
                    }
                }

                // Get value from value itself
                if (typeof output == 'undefined') {
                    output = value;
                }

                // Add extra
                if (typeof output != 'undefined' && this.extra) {
                    output = `${ output }${ this.extra(player) }`;
                }

                if (typeof output == 'undefined') {
                    output = '';
                }

                // Replace spaces with unbreakable ones
                if (this.breakline == 0) {
                    output = output.replace(/\ /g, '&nbsp;')
                }

                // Return value
                return output;
            },
            getDifference: function (player, compare, settings, value, extra = undefined) {
                let nativeDifference = Number.isInteger(value) ? value : value.toFixed(2);

                if (this.formatDifference === true) {
                    if (this.format instanceof Expression) {
                        return new ExpressionScope(settings).with(player, compare).addSelf(value).add(extra).eval(this.format);
                    } else if (typeof this.format == 'function') {
                        return this.format(player, compare, settings, value, extra);
                    } else {
                        return nativeDifference;
                    }
                } else if (this.formatDifference instanceof Expression) {
                    return new ExpressionScope(settings).with(player, compare).addSelf(value).add(extra).eval(this.formatDifference);
                } else if (typeof this.formatDifference == 'function') {
                    return this.formatDifference(settings, value);
                } else {
                    return nativeDifference;
                }
            },
            getStatistics: function (settings, value) {
                let nativeFormat = Number.isInteger(value) ? value : value.toFixed(2);

                if (this.formatStatistics === false) {
                    return nativeFormat;
                } else if (this.formatStatistics) {
                    return new ExpressionScope(settings).addSelf(value).eval(this.formatStatistics);
                } else if (this.format instanceof Expression) {
                    return new ExpressionScope(settings).addSelf(value).eval(this.format);
                } else if (typeof this.format == 'function') {
                    return this.format(undefined, undefined, settings, value);
                } else {
                    return nativeFormat;
                }
            }
        }
    }

    // Create new header
    addHeader (name, grouped = 0) {
        this.push();

        // Header
        this.header = {
            name: name,
            value: this.getValueBlock(),
            color: this.getColorBlock()
        };

        if (grouped) {
            this.header.grouped = grouped;
        }
    }

    // Create new category
    addCategory (name) {
        this.pushCategory();

        // Category
        this.category = {
            name: name,
            empty: name == '',
            headers: []
        };

        // Category shared
        this.sharedCategory = { }
    }

    // Create row
    addRow (name, expression) {
        this.push();

        // Row
        this.row = {
            name: name,
            ast: expression,
            value: this.getValueBlock(),
            color: this.getColorBlock()
        }
    }

    // Create definition
    addDefinition (name) {
        this.push();

        // Definition
        this.definition = {
            name: name,
            value: this.getValueBlock(),
            color: this.getColorBlock()
        }
    }

    // Create statistic
    addStatistics (name, expression) {
        this.customStatistics.push({
            name: name,
            ast: expression
        });
    }

    // Add alias
    addAlias (name) {
        let object = (this.definition || this.header || this.embed);
        if (object) {
            object.alias = name;
        }
    }

    // Add custom style
    addStyle (name, value) {
        let object = (this.row || this.definition || this.header || this.embed || this.sharedCategory || this.shared);
        if (object) {
            if (!object.style) {
                object.style = new CellStyle();
            }

            object.style.add(name, value);
        }
    }

    // Add color expression to the header
    addColorExpression (expression) {
        let object = (this.row || this.definition || this.header || this.embed);
        if (object) {
            object.color.expr = expression;
        }
    }

    addAliasExpression (expression) {
        let object = (this.row || this.definition || this.header || this.embed || this.category);
        if (object) {
            object['expa'] = expression;
        }
    }

    addGlobOrder (index, order) {
        let object = (this.header || this.embed);
        if (object) {
            object['glob_order'] = {
                ord: order,
                index: index
            }
        }
    }

    // Add format expression to the header
    addFormatExpression (expression) {
        let object = (this.row || this.definition || this.header || this.embed);
        if (object) {
            object.value.format = expression;
        }
    }

    addBreaklineRule (value) {
        let object = (this.row || this.definition || this.header || this.embed);
        if (object) {
            object.value.breakline = value;
        }
    }

    // Add format extra expression to the header
    addFormatExtraExpression (expression) {
        let object = (this.row || this.definition || this.header || this.embed);
        if (object) {
            object.value.extra = expression;
        }
    }

    addFormatStatisticsExpression (expression) {
        let object = (this.row || this.definition || this.header || this.embed);
        if (object) {
            object.value.formatStatistics = expression;
        }
    }

    addFormatDifferenceExpression (expression) {
        let object = (this.row || this.definition || this.header || this.embed);
        if (object) {
            object.value.formatDifference = expression;
        }
    }

    // Add color rule to the header
    addColorRule (condition, referenceValue, value) {
        let object = (this.row || this.definition || this.header || this.embed);
        if (object) {
            object.color.rules.addRule(condition, referenceValue, value);
        }
    }

    // Add value rule to the header
    addValueRule (condition, referenceValue, value) {
        let object = (this.row || this.definition || this.header || this.embed);
        if (object) {
            object.value.rules.addRule(condition, referenceValue, value);
        }
    }

    // Add new variable
    addVariable (name, expression, isTableVariable = false) {
        this.variables[name] = {
            ast: expression,
            tableVariable: isTableVariable
        }
    }

    // Add new function
    addFunction (name, expression, args) {
        this.functions[name] = {
            ast: expression,
            args: args
        }
    }

    // Add global
    addGlobal (name, value) {
        this.globals[name] = value;
    }

    addGlobalEmbedable (name, value) {
        (this.embed || this.definition || this.globals)[name] = value;
    }

    // Add shared variable
    addShared (name, value) {
        let object = (this.row || this.definition || this.header || this.embed || this.sharedCategory || this.shared);
        if (object) {
            object[name] = value;
        }

        this.addLocal(`ex_${ name }`, value);
    }

    // Add extension
    addExtension (... names) {
        let object = (this.row || this.definition || this.header || this.embed || this.sharedCategory || this.shared);
        if (object) {
            if (!object.extensions) {
                object.extensions = [];
            }

            object.extensions.push(... names);
        }
    }

    // Add constant
    addConstant (name, value) {
        this.constants.addConstant(name, value);
    }

    // Add local variable
    addLocal (name, value) {
        let object = (this.row || this.definition || this.header || this.embed);
        if (object) {
            object[name] = value;
        }
    }

    // Add action
    addAction (value) {
        let object = (this.header || this.embed);
        if (object) {
            object['action'] = value;
        }
    }

    addHeaderVariable (name, value) {
        let object = (this.row || this.definition || this.header || this.embed || this.sharedCategory || this.shared);
        if (object) {
            if (!object.vars) {
                object.vars = {};
            }

            object.vars[name] = value;
        }
    }

    embedBlock (name) {
        this.push();

        this.embed = {
            name,
            embedded: true,
            headers: [],
            value: this.getValueBlock(),
            color: this.getColorBlock()
        }
    }

    pushEmbed () {
        let obj = this.embed;
        if (obj && this.category) {
            this.push();

            for (let definitionName of obj.extensions || []) {
                this.mergeDefinition(obj, definitionName);
            }

            if (!obj.clean) {
                this.merge(obj, this.sharedCategory);
                this.merge(obj, this.shared);
            } else {
                this.merge(obj, {
                    visible: true,
                    statistics_color: true
                });
            }

            if (obj.background) {
                obj.color.rules.addRule('db', 0, obj.background);
            }

            if (this.category) {
                this.category.headers.push(obj);
                this.embed = null;
            }
        }
    }

    addLayout (layout) {
        this.globals.layout = layout;
    }

    // Add discard rule
    addDiscardRule (rule) {
        this.discardRules.push(rule);
    }

    addDefaultOrder (order) {
        this.globals.order_by = order;
    }

    // Get code
    getCode () {
        return this.code;
    }

    // Get environment
    getEnvironment () {
        return this;
    }

    // Get compare environment
    getCompareEnvironment () {
        return {
            functions: this.functions,
            variables: this.variablesReference,
            lists: {},
            array: [],
            array_unfiltered: [],
            // Constants have to be propagated through the environment
            constants: this.constants,
            row_indexes: this.row_indexes,
            timestamp: this.reference,
            reference: this.reference,
            env_id: this.env_id
        }
    }

    // Random getters and stuff
    getIndexStyle () {
        return this.globals.indexed;
    }

    getServerStyle () {
        return this.globals.server == undefined ? 100 : this.globals.server;
    }

    getBackgroundStyle () {
        return this.shared.background;
    }

    getOutdatedStyle () {
        return this.globals.outdated;
    }

    getLayout (hasStatistics, hasRows, hasMembers) {
        if (typeof this.globals.layout != 'undefined') {
            return this.globals.layout;
        } else {
            if (this.type == ScriptType.Players) {
                return [
                    ... (hasStatistics ? [ 'statistics', hasRows ? '|' : '_' ] : []),
                    ... (hasRows ? (hasStatistics ? [ 'rows', '_' ] : [ 'rows', '|', '_' ]) : []),
                    'table'
                ];
            } else if (this.type == ScriptType.Group) {
                return [
                    'table',
                    ... (hasStatistics || hasRows || hasMembers ? [ '_' ] : []),
                    ... (hasStatistics ? [ 'statistics' ] : []),
                    ... (hasRows ? [ '|', 'rows' ] : []),
                    ... (hasMembers ? [ '|', 'members' ] : [])
                ];
            } else {
                return [
                    ... (hasRows ? [ 'rows', '|', '_' ] : []),
                    'table'
                ];
            }
        }
    }

    getEntryLimit () {
        return this.globals.performance;
    }

    getSimulatorLimit () {
        return this.globals.simulator;
    }

    getOpaqueStyle () {
        return this.globals.opaque ? 'css-entry-opaque' : '';
    }

    getLinedStyle () {
        return this.globals.lined || 0;
    }

    getRowStyle () {
        return this.globals['large rows'] ? 'css-maxi-row' : '';
    }

    getRowHeight () {
        return this.globals['row_height'] || 0;
    }

    getFontStyle () {
        return this.globals.font ? `font: ${ this.globals.font };` : '';
    }

    getTitleAlign () {
        return this.globals['align title'];
    }

    getNameStyle () {
        return Math.max(100, this.globals.name == undefined ? 250 : this.globals.name);
    }

    evalRowIndexes (array, embedded = false) {
        // For every entry
        array.forEach((entry, index) => {
            // Get player from object if embedded
            let player = embedded ? entry.player : entry;

            // Save player index
            this.row_indexes[`${ player.Identifier }_${ player.Timestamp }`] = index;
        });
    }

    evalRules () {
        // For each category
        for (let category of this.categories) {
            // For each header
            for (let header of category.headers) {
                // For each rule block
                for (let rules of [ header.color.rules.rules, header.value.rules.rules ]) {
                    // For each entry
                    for (let i = 0, rule; rule = rules[i]; i++) {
                        let key = rule[3];
                        // Check if key exists
                        if (key && key in this.variables) {
                            // If variable with that name exists then set it
                            if (this.variables[key].value != 'undefined') {
                                // Set value
                                rule[1] = Number(this.variables[key].value);
                            } else {
                                // Remove the rule
                                rules.splice(i--, 1);
                            }
                        }
                    }
                }
            }
        }
    }

    createSegmentedArray (array, mapper) {
        let segmentedArray = array.map((entry, index, arr) => {
            let obj = mapper(entry, index, arr);
            obj.segmented = true;
            return obj;
        });

        segmentedArray.segmented = true;

        return segmentedArray;
    }

    evalHistory (array, unfilteredArray) {
        // Evaluate row indexes
        this.evalRowIndexes(array);

        // Purify array
        array = [ ... array ];

        // Get shared scope
        let scope = this.createSegmentedArray(array, (player, index, arr) => [player, arr[index + 1] || player]);
        let unfilteredScope = this.createSegmentedArray(unfilteredArray, (player, index, arr) => [player, arr[index + 1] || player]);

        this.array = array;
        this.array_unfiltered = unfilteredArray;

        // Iterate over all variables
        for (let [ name, variable ] of Object.entries(this.variables)) {
            // Copy over to reference variables
            this.variablesReference[name] = {
                ast: variable.ast,
                tableVariable: variable.tableVariable
            }

            // Run only if it is a table variable
            if (variable.tableVariable) {
                // Get value
                let value = new ExpressionScope(this).addSelf(variable.tableVariable == 'unfiltered' ? unfilteredScope : scope).eval(variable.ast);

                // Set value if valid
                if (!isNaN(value) || typeof(value) == 'object' || typeof('value') == 'string') {
                    variable.value = value;
                } else {
                    delete variable.value;
                }
            }
        }

        // Evaluate custom rows
        for (let row of this.customRows) {
            let currentValue = new ExpressionScope(this).with(array[0]).addSelf(array).eval(row.ast);

            row.eval = {
                value: currentValue
            }
        }

        // Evaluate array constants
        this.evalRules();
    }

    evalPlayers (array, unfilteredArray, simulatorLimit, entryLimit) {
        // Evaluate row indexes
        this.evalRowIndexes(array, true);

        // Variables
        let compareEnvironment = this.getCompareEnvironment();
        let sameTimestamp = array.timestamp == array.reference;

        // Set lists
        this.lists = {
            classes: array.reduce((c, { player }) => {
                c[player.Class]++;
                return c;
            }, {
                1: 0,
                2: 0,
                3: 0,
                4: 0,
                5: 0,
                6: 0,
                7: 0,
                8: 0
            })
        }

        this.array = array;
        this.array_unfiltered = unfilteredArray;
        this.timestamp = array.timestamp;
        this.reference = array.reference;

        // Run simulator if needed
        this.evalSimulator(array, simulatorLimit, entryLimit);

        // Get segmented lists
        let arrayCurrent = this.createSegmentedArray(array, entry => [entry.player, entry.compare]);
        let arrayCompare = this.createSegmentedArray(array, entry => [entry.compare, entry.compare]);
        let unfilteredArrayCurrent = this.createSegmentedArray(unfilteredArray, entry => [entry.player, entry.compare]);
        let unfilteredArrayCompare = this.createSegmentedArray(unfilteredArray, entry => [entry.compare, entry.compare]);

        // Evaluate variables
        for (let [ name, variable ] of Object.entries(this.variables)) {
            // Copy over to reference variables
            this.variablesReference[name] = {
                ast: variable.ast,
                tableVariable: variable.tableVariable
            }

            if (variable.tableVariable) {
                // Calculate values of table variable
                let currentValue = new ExpressionScope(this).addSelf(variable.tableVariable == 'unfiltered' ? unfilteredArrayCurrent : arrayCurrent).eval(variable.ast);
                let compareValue = sameTimestamp ? currentValue : new ExpressionScope(this).addSelf(variable.tableVariable == 'unfiltered' ? unfilteredArrayCompare : arrayCompare).eval(variable.ast);

                // Set values if valid
                if (!isNaN(currentValue) || typeof currentValue == 'object' || typeof currentValue == 'string') {
                    variable.value = currentValue;
                } else {
                    delete variable.value;
                }

                if (!isNaN(compareValue) || typeof compareValue == 'object' || typeof compareValue == 'string') {
                    this.variablesReference[name].value = compareValue;
                } else {
                    delete this.variablesReference[name].value;
                }
            }
        }

        // Evaluate custom rows
        for (let row of this.customRows) {
            let currentValue = new ExpressionScope(this).addSelf(arrayCurrent).eval(row.ast);
            let compareValue = sameTimestamp ? currentValue : new ExpressionScope(compareEnvironment).addSelf(arrayCompare).eval(row.ast);

            row.eval = {
                value: currentValue,
                compare: compareValue
            }
        }

        // Evaluate array constants
        this.evalRules();
    }

    evalGuilds (array, unfilteredArray) {
        // Evaluate row indexes
        this.evalRowIndexes(array, true);

        // Variables
        let simulatorLimit = this.getSimulatorLimit();
        let entryLimit = array.length;
        let compareEnvironment = this.getCompareEnvironment();
        let sameTimestamp = array.timestamp == array.reference;

        // Set lists
        this.lists = {
            joined: SiteOptions.obfuscated ? array.joined.map((p, i) => `joined_${ i + 1 }`) : array.joined,
            kicked: SiteOptions.obfuscated ? array.kicked.map((p, i) => `kicked_${ i + 1 }`) : array.kicked,
            classes: array.reduce((c, { player }) => {
                c[player.Class]++;
                return c;
            }, {
                1: 0,
                2: 0,
                3: 0,
                4: 0,
                5: 0,
                6: 0,
                7: 0,
                8: 0
            })
        }

        this.array = array;
        this.array_unfiltered = unfilteredArray;
        this.timestamp = array.timestamp;
        this.reference = array.reference;

        // Run simulator if needed
        this.evalSimulator(array, simulatorLimit, entryLimit);

        array = [ ... array ];
        unfilteredArray = [ ... unfilteredArray ];

        // Get segmented lists
        let arrayCurrent = this.createSegmentedArray(array, entry => [entry.player, entry.compare]);
        let arrayCompare = this.createSegmentedArray(array, entry => [entry.compare, entry.compare]);
        let unfilteredArrayCurrent = this.createSegmentedArray(unfilteredArray, entry => [entry.player, entry.compare]);
        let unfilteredArrayCompare = this.createSegmentedArray(unfilteredArray, entry => [entry.compare, entry.compare]);

        // Get own player
        let ownEntry = array.find(entry => entry.player.Own) || array[0];
        let ownPlayer = _dig(ownEntry, 'player');
        let ownCompare = _dig(ownEntry, 'compare');

        // Evaluate variables
        for (let [ name, variable ] of Object.entries(this.variables)) {
            // Copy over to reference variables
            this.variablesReference[name] = {
                ast: variable.ast,
                tableVariable: variable.tableVariable
            }

            if (variable.tableVariable) {
                // Calculate values of table variable
                let currentValue = new ExpressionScope(this).addSelf(variable.tableVariable == 'unfiltered' ? unfilteredArrayCurrent : arrayCurrent).eval(variable.ast);
                let compareValue = sameTimestamp ? currentValue : new ExpressionScope(this).addSelf(variable.tableVariable == 'unfiltered' ? unfilteredArrayCompare : arrayCompare).eval(variable.ast);


                // Set values if valid
                if (!isNaN(currentValue) || typeof currentValue == 'object' || typeof currentValue == 'string') {
                    variable.value = currentValue;
                } else {
                    delete variable.value;
                }

                if (!isNaN(compareValue) || typeof compareValue == 'object' || typeof compareValue == 'string') {
                    this.variablesReference[name].value = compareValue;
                } else {
                    delete this.variablesReference[name].value;
                }
            }
        }

        // Evaluate custom rows
        for (let row of this.customRows) {
            let currentValue = new ExpressionScope(this).with(ownPlayer, ownCompare).addSelf(arrayCurrent).eval(row.ast);
            let compareValue = sameTimestamp ? currentValue : new ExpressionScope(compareEnvironment).with(ownCompare, ownCompare).addSelf(arrayCompare).eval(row.ast);

            row.eval = {
                value: currentValue,
                compare: compareValue
            }
        }

        this.evalRules();
    }

    fetchSimulatorPlayer ({ Identifier: identifier, Timestamp: timestamp }) {
        let player = DatabaseManager.getPlayer(identifier, timestamp).toSimulatorModel();
        player.ForceGladiator = 15;

        return {
            player
        };
    }

    evalSimulator (array, cycles = 0, limit = array.length) {
        if (cycles) {
            // Check whether timestamps match
            let sameTimestamp = array.reference == array.timestamp;

            // Slice the array depending on the entry limit
            array = array.slice(0, limit);

            // Arrays
            let arrayCurrent = null;
            let arrayCompare = null;

            // Simulate
            if (sameTimestamp) {
                // Create arrays
                arrayCurrent = array.map(({ player }) => this.fetchSimulatorPlayer(player));

                // Set target
                let targetCurrent = this.globals.simulator_target ? arrayCurrent.find(({ player }) => player.Identifier == this.globals.simulator_target) : null;

                // Null the targets if any is not found
                if (targetCurrent == null) {
                    targetCurrent = null;
                } else {
                    targetCurrent = targetCurrent.player;
                }

                // Run fight simulator
                new FightSimulator().simulate(arrayCurrent, cycles, targetCurrent, this.globals.simulator_target_source);
            } else {
                // Create arrays
                arrayCurrent = array.map(({ player }) => this.fetchSimulatorPlayer(player));
                arrayCompare = array.map(({ compare }) => this.fetchSimulatorPlayer(compare));

                // Set targets
                let targetCurrent = this.globals.simulator_target ? arrayCurrent.find(({ player }) => player.Identifier == this.globals.simulator_target) : null;
                let targetCompare = this.globals.simulator_target ? arrayCurrent.find(({ player }) => player.Identifier == this.globals.simulator_target) : null;

                // Null the targets if any is not found
                if (targetCurrent == null || targetCompare == null) {
                    targetCompare = targetCompare = null;
                } else {
                    targetCurrent = targetCurrent.player;
                    targetCompare = targetCompare.player;
                }

                // Run fight simulator
                new FightSimulator().simulate(arrayCurrent, cycles, targetCurrent, this.globals.simulator_target_source);
                new FightSimulator().simulate(arrayCompare, cycles, targetCompare, this.globals.simulator_target_source);
            }

            // Set second array to first if missing
            if (sameTimestamp) {
                arrayCompare = arrayCurrent;
            }

            // Process results
            let resultsCurrent = {};
            let resultsCompare = {};

            for (let { player, score } of arrayCurrent) {
                resultsCurrent[player.Identifier] = score;
            }

            for (let { player, score } of arrayCompare) {
                resultsCompare[player.Identifier] = score;
            }

            // Save variables
            this.variables['Simulator'] = {
                value: resultsCurrent
            };

            this.variablesReference['Simulator'] = {
                value: resultsCompare
            }
        } else {
            // Delete variables
            delete this.variables['Simulator'];
            delete this.variablesReference['Simulator'];
        }
    }


    /*
        Old shit
    */

    static parseConstants(string, type) {
        let settings = new Settings('');

        for (var line of Settings.handleMacros(string)) {
            let trimmed = Settings.stripComments(line)[0].trim();

            for (let command of SettingsCommands) {
                if (command.canParse && command.canParseAlways && command.type == type && command.isValid(trimmed)) {
                    command.parse(settings, trimmed);
                    break;
                }
            }
        }

        return settings;
    }

    static checkEscapeTrail (line, index) {
        if (line[index - 1] != '\\') {
            return false;
        } else {
            let escape = true;
            for (let i = index - 2; i >= 0 && line[i] == '\\'; i--) {
                escape = !escape;
            }

            return escape;
        }
    }

    static stripComments (line, escape = true) {
        let comment;
        let commentIndex = -1;

        let ignored = false;
        for (var i = 0; i < line.length; i++) {
            if (line[i] == '\'' || line[i] == '\"' || line[i] == '\`') {
                if (line[i - 1] == '\\' || (ignored && line[i] != ignored)) continue;
                else {
                    ignored = ignored ? false : line[i];
                }
            } else if (!Settings.checkEscapeTrail(line, i) && line[i] == '#' && !ignored) {
                commentIndex = i;
                break;
            }
        }

        if (commentIndex != -1) {
            comment = line.slice(commentIndex);
            line = line.slice(0, commentIndex);

            if (escape) {
                line = line.replaceAll(/\\(\\|#)/g, (_, capture) => {
                    commentIndex -= 1;
                    return capture;
                });
            }
        } else if (escape) {
            line = line.replaceAll(/\\(\\|#)/g, (_, capture) => capture);
        }

        return [ line, comment, commentIndex ];
    }

    // Format code
    static format (string, type = 0) {
        var settings = Settings.parseConstants(string, type);
        var content = '';

        SettingsHighlightCache.setCurrent(settings);

        for (let line of string.split('\n')) {
            if (SettingsHighlightCache.has(line)) {
                content += SettingsHighlightCache.get(line);
            } else {
                let [ commandLine, comment, commentIndex ] = Settings.stripComments(line, false);
                let [ , prefix, trimmed, suffix ] = commandLine.match(/^(\s*)(\S(?:.*\S)?)?(\s*)$/);

                let currentContent = '';
                currentContent += prefix.replace(/ /g, '&nbsp;');

                if (trimmed) {
                    let command = SettingsCommands.find(command => command.isValid(trimmed));
                    if (command && command.type == type) {
                        currentContent += command.format(settings, trimmed);
                    } else {
                        currentContent += SFormat.Error(trimmed);
                    }
                }

                currentContent += suffix.replace(/ /g, '&nbsp;');
                if (commentIndex != -1) {
                    currentContent += SFormat.Comment(comment);
                }

                SettingsHighlightCache.store(line, currentContent);

                content += currentContent;
            }

            content += '</br>';
        }

        return content;
    }
};

const SettingsHighlightCache = new (class {
    initialize () {
        this.hash = null;
        this.cache = {};
    }

    setCurrent (tempSettings) {
        let newHash = SHA1(JSON.stringify(tempSettings));
        if (newHash != this.hash) {
            this.hash = newHash;
            this.cache = {};
        }
    }

    has (line) {
        return line in this.cache;
    }

    get (line) {
        return this.cache[line];
    }

    store (line, output) {
        this.cache[line] = output;
    }
})();

// Settings manager
const SettingsManager = new (class {
    initialize () {
        if (!this.settings) {
            // Initialize if needed
            this.settings = Preferences.get('settings', { });

            if (typeof this.settings === 'string') {
                Preferences.set('settingsStringRaw', this.settings);

                this.settings = {};
                Preferences.set('settings', {});
            }

            this.keys = Object.keys(this.settings);

            /*
                Convert existing settings in old form into new style
            */
            let keys = Preferences.keys().filter(key => key.includes('settings/')).map(key => key.substring(key.indexOf('/') + 1));
            let backup = {};

            if (keys.length) {
                for (let key of keys) {
                    let content = Preferences.get(`settings/${ key }`, '');
                    backup[key] = content;

                    // Convert existing settings to text
                    if (typeof content == 'string') {
                        // Do nothing
                    } else {
                        content = '';
                    }

                    // Save if valid
                    if (content.length) {
                        this.saveInternal(key, content);
                    }

                    Preferences.remove(`settings/${ key }`);
                }

                this.commit();
                Preferences.set('settingsBackup', backup);
            }
        }
    }

    tracker () {
        return new Settings(this.get('tracker', '', ''), ScriptType.Tracker);
    }

    trackerConfig () {
        return this.tracker().trackers;
    }

    commit () {
        // Save current settings
        Preferences.set('settings', this.settings);
        this.keys = Object.keys(this.settings);
    }

    saveInternal (name, content, parent = '') {
        // Check if settings exist
        let exists = name in this.settings;
        let settings = exists ? this.settings[name] : null;

        if (exists) {
            settings.content = content;
            settings.version = MODULE_VERSION;
            settings.timestamp = Date.now();
            settings.parent = parent;
        } else {
            settings = {
                name: name,
                content: content,
                parent: parent,
                version: MODULE_VERSION,
                timestamp: Date.now()
            }
        }

        this.settings[name] = settings;
    }

    save (name, content, parent) {
        this.initialize();
        this.saveInternal(name, content, parent);
        this.commit();
    }

    remove (name) {
        this.initialize();
        if (name in this.settings) {
            delete this.settings[name];
            this.commit();
        }
    }

    exists (name) {
        this.initialize();
        return name in this.settings;
    }

    all () {
        this.initialize();
        return this.settings;
    }

    list () {
        this.initialize();
        return Object.values(this.settings);
    }

    getKeys () {
        this.initialize();
        return this.keys;
    }

    get (name, fallback, template) {
        this.initialize();
        let settings = this.settings[name] || this.settings[fallback];
        return settings ? settings.content : template;
    }

    getObj (name, fallback) {
        this.initialize();
        return this.settings[name] || this.settings[fallback];
    }

    /*
        History stuff
    */

    // Get history
    getHistory () {
        return Preferences.get('settings_history', []);
    }

    // Add history
    addHistory (settings, identifier = 'settings') {
        // Get current history
        var history = Preferences.get('settings_history', []);

        // Add new history entry to the beginning
        history.unshift({
            name: identifier,
            content: settings
        });

        // Pop last entry if over 10 entries exist
        if (history.length > 10) {
            history.pop();
        }

        // Save current history
        Preferences.set('settings_history', history);
    }
})()

// Templates
const Templates = new (class {
    initialize () {
        if (!this.templates) {
            // Initialize when needed
            this.templates = SharedPreferences.get('templates', { });

            this.keys = Object.keys(this.templates);
            this.keys.sort((a, b) => a.localeCompare(b));

            /*
                Convert existing templates in old form into new style
            */
            let keys = SharedPreferences.keys().filter(key => key.includes('templates/')).map(key => key.substring(key.indexOf('/') + 1));
            let backup = {};

            if (keys.length) {
                for (let key of keys) {
                    let content = SharedPreferences.get(`templates/${ key }`, '');
                    backup[key] = content;

                    // Convert existing template to text
                    if (typeof(content) == 'string') {
                        // Do nothing
                    } else if (typeof(content) == 'object') {
                        content = content.content;
                    } else {
                        content = '';
                    }

                    // Save if valid
                    if (content.length) {
                        this.saveInternal(key, content);
                    }

                    SharedPreferences.remove(`templates/${ key }`);
                }

                this.commit();
                SharedPreferences.set('templatesBackup', backup);
            }
        }
    }

    commit () {
        // Save current templates
        SharedPreferences.set('templates', this.templates);

        this.keys = Object.keys(this.templates);
        this.keys.sort((a, b) => a.localeCompare(b));
    }

    saveInternal (name, content, compat = { cm: false, cg: false, cp: false }) {
        // Check if a template already exists
        let exists = name in this.templates;
        let template = exists ? this.templates[name] : null;

        if (exists) {
            // Overwrite needed parts
            template.content = content;
            template.compat = compat;
            template.version = MODULE_VERSION;
            template.timestamp = Date.now();
        } else {
            // Create new object
            template = {
                name: name,
                content: content,
                compat: compat,
                version: MODULE_VERSION,
                timestamp: Date.now(),
                online: false
            };
        }

        // Save template
        this.templates[name] = template;
    }

    markAsOnline (name, key, secret) {
        this.initialize();

        // Mark template as online if exists
        if (name in this.templates) {
            // Set timestamp & keys
            this.templates[name].online = {
                timestamp: this.templates[name].timestamp,
                key: key,
                secret: secret
            };

            this.commit();
        }
    }

    markAsOffline (name) {
        this.initialize();

        // Mark template as offline if exists
        if (name in this.templates) {
            // Set online to false
            this.templates[name].online = false;

            this.commit();
        }
    }

    save (name, content, compat) {
        this.initialize();

        // Save template
        this.saveInternal(name, content, compat);

        // Commit changes
        this.commit();
    }

    remove (name) {
        this.initialize();

        // Remove template
        if (name in this.templates) {
            delete this.templates[name];
            this.commit();
        }
    }

    exists (name) {
        this.initialize();

        // Return true if template exists
        return name in this.templates;
    }

    all () {
        this.initialize();

        // Return templates
        return this.templates;
    }

    list () {
        this.initialize();

        // Return list of templates
        return Object.values(this.templates);
    }

    getKeys () {
        this.initialize();

        // Return keys
        return this.keys;
    }

    get (name) {
        this.initialize();

        // Return loaded settings
        return name in this.templates ? this.templates[name].content : '';
    }
})();

class ScriptEditor {
    constructor (parent, editorType, changeCallback) {
        this.changeCallback = changeCallback;
        this.editorType = editorType;

        this.$parent = parent;
        this.$area = this.$parent.find('textarea');
        this.$wrapper = this.$parent.find('.ta-wrapper');
        this.$mask = this.$parent.find('.ta-content');

        this.$mask.css('top', this.$area.css('padding-top'));
        this.$mask.css('left', this.$area.css('padding-left'));
        this.$mask.css('font', this.$area.css('font'));
        this.$mask.css('font-family', this.$area.css('font-family'));
        this.$mask.css('line-height', this.$area.css('line-height'));

        let $maskClone = this.$mask.clone();

        this.$area.on('input', (event) => {
            var val = $(event.currentTarget).val();
            if (this.pasted) {
                val = val.replace(/\t/g, ' ');

                var ob = this.$area.get(0);

                var ob1 = ob.selectionStart;
                var ob2 = ob.selectionEnd;
                var ob3 = ob.selectionDirection;

                ob.value = val;

                ob.selectionStart = ob1;
                ob.selectionEnd = ob2;
                ob.selectionDirection = ob3;

                this.pasted = false;
            }

            let scrollTransform = this.$mask.css('transform');
            this.$mask.remove();
            this.$mask = $maskClone.clone().html(Settings.format(val, this.editorType)).css('transform', scrollTransform).appendTo(this.$wrapper);

            if (typeof this.changeCallback === 'function') {
                this.changeCallback(val);
            }
        }).trigger('input');

        this.$area.on('scroll', (event) => {
            var sy = $(event.currentTarget).scrollTop();
            var sx = $(event.currentTarget).scrollLeft();
            this.$mask.css('transform', `translate(${ -sx }px, ${ -sy }px)`);
        });

        this.$area.keydown((e) => {
            if (e.key == 'Tab') {
                e.preventDefault();

                let a = this.$area.get(0);
                let v = this.$area.val();
                let s = a.selectionStart;
                let d = a.selectionEnd;

                if (s == d) {
                    this.$area.val(v.substring(0, s) + '  ' + v.substring(s));
                    a.selectionStart = s + 2;
                    a.selectionEnd = d + 2;
                } else {
                    let o = 0, oo = 0, i;
                    for (i = d - 1; i > s; i--) {
                        if (v[i] == '\n') {
                            v = v.substring(0, i + 1) + '  ' + v.substring(i + 1);
                            oo++;
                        }
                    }

                    while (i >= 0) {
                        if (v[i] == '\n') {
                            v = v.substring(0, i + 1) + '  ' + v.substring(i + 1);
                            o++;
                            break;
                        } else {
                            i--;
                        }
                    }

                    this.$area.val(v);
                    a.selectionStart = s + o * 2;
                    a.selectionEnd = d + (oo + o) * 2;
                }

                this.$area.trigger('input');
            }
        });

        this.$area.on('paste', () => {
            this.pasted = true;
        });

        this.$area.on('dragover dragenter', e => {
            e.preventDefault();
            e.stopPropagation();
        }).on('drop', e => {
            if (_dig(e, 'originalEvent', 'dataTransfer', 'files', 0, 'type') == 'text/plain') {
                e.preventDefault();
                e.stopPropagation();

                let r = new FileReader();
                r.readAsText(e.originalEvent.dataTransfer.files[0], 'UTF-8');
                r.onload = f => {
                    this.$area.val(f.target.result).trigger('input');
                }
            }
        });
    }

    get content () {
        return this.$area.val();
    }

    set content (val) {
        this.$area.val(val);
        this.$area.trigger('input');
    }

    scrollTop () {
        this.$area.scrollTop(0).trigger('scroll');
    }
}
