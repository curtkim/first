// Generated by CoffeeScript 1.10.0
(function() {
  var c, data, i, k, l, m, name, nodeMap, o, ref, root, s, t;

  data = [
    {
      7: 1,
      8: 1,
      9: 3,
      10: 4,
      11: 4,
      12: 6
    }, {
      13: 7,
      14: 7,
      15: 9,
      16: 11,
      17: 12,
      18: 12
    }
  ];

  root = {
    name: 'root',
    parent: null,
    children: []
  };

  nodeMap = {};

  for (t = k = 0; k <= 2; t = ++k) {
    for (c = l = 0; l <= 5; c = ++l) {
      name = t * 6 + c + 1;
      nodeMap[name] = {
        name: name,
        parent: null,
        children: []
      };
    }
  }

  for (t = m = 1; m >= 0; t = --m) {
    ref = data[t];
    for (t in ref) {
      s = ref[t];
      nodeMap[t].parent = nodeMap[s];
      nodeMap[s].children.push(nodeMap[t]);
    }
  }

  for (i = o = 1; o <= 6; i = ++o) {
    nodeMap[i].parent = root;
    root.children.push(nodeMap[i]);
  }

  $(document).ready(function() {
    var diagonal, height, margin, svg, tree, update, width;
    margin = {
      top: 20,
      right: 120,
      bottom: 20,
      left: 120
    };
    width = 960 - margin.right - margin.left;
    height = 500 - margin.top - margin.bottom;
    i = 0;
    tree = d3.layout.tree().size([height, width]);
    diagonal = d3.svg.diagonal().projection(function(d) {
      return [d.y, d.x];
    });
    svg = d3.select('body').append('svg').attr('width', width + margin.right + margin.left).attr('height', height + margin.top + margin.bottom).append('g').attr('transform', "translate(" + margin.left + "," + margin.top + ")");
    update = function(source) {
      var j, len, link, links, n, node, nodeEnter, nodes, p;
      nodes = tree.nodes(root).reverse();
      links = tree.links(nodes);
      for (p = 0, len = nodes.length; p < len; p++) {
        n = nodes[p];
        if (!(n.name !== 'root')) {
          continue;
        }
        t = parseInt((n.name - 1) / 6);
        j = (n.name - 1) % 6;
        n.x = (j + 1) * 50;
      }
      nodes.forEach(function(d) {
        return d.y = d.depth * 100;
      });
      node = svg.selectAll('g.node').data(nodes, function(d) {
        return d.id || (d.id = ++i);
      });
      nodeEnter = node.enter().append('g').attr('class', 'node').attr('transform', function(d) {
        return "translate(" + d.y + "," + d.x + ")";
      });
      nodeEnter.append('circle').attr('r', 10).style('fill', '#fff');
      nodeEnter.append('text').attr('x', function(d) {
        if (d.children || d._children) {
          return -13;
        } else {
          return 13;
        }
      }).attr('dy', '.35em').attr('text-anchor', function(d) {
        if (d.children || d._children) {
          return 'end';
        } else {
          return 'start';
        }
      }).text(function(d) {
        return d.name;
      }).style('fill-opacity', 1);
      link = svg.selectAll('path.link').data(links, function(d) {
        return d.target.id;
      });
      return link.enter().insert('path', 'g').attr('class', 'link').attr('d', diagonal);
    };
    return update(root);
  });

}).call(this);