#!/usr/bin/python
import random
import time



def defaultActivityHook(startTime, attrs):
    t = startTime
    delta1 = random.randint(1, 15*60)
    delta2 = random.randint(delta1+1, 30*60)
    delta3 = random.randint(delta2+1, 45*60)
    delta4 = random.randint(delta3+1, 59*60)
    t0 = t+delta1
    t1 = t+delta2
    t2 = t+delta3
    t3 = t+delta4
    return t0, t1, t2, t3
    
def defaultChoiceHook(l, attr):
    x = random.randint(0, len(l)-1)
    return l[x]

class Empty():
    def __init__(self):
        pass
    def gen(self, t, attrs):
        return ("", t)

class Entry:
    def __init__(self, name, hook=None):
        self.name = name
        self.hook = hook
        if self.hook is None:
            self.hook = defaultActivityHook

    def formatAttr(self, key, value):
        if type(value) == bool:
            valueRepr = "true" if value else "false"
            typeRepr = "BOOLEAN"
        elif type(value) == float:
            valueRepr = str(value)
            typeRepr = "CONTINUOUS"
        elif type(value) == int:
            valueRepr = str(value)
            typeRepr = "DISCRETE"
        else:
            valueRepr = str(value)
            typeRepr = "LITERAL"
        #TIMESTAMP: Attributes with timestamp type encode the UNIX timestamp (e.g., milliseconds since 01/01/1970) they represent, in the same form as discrete attributes.
        #DURATION: Attributes with duration type encode their values in milliseconds, in the same form as discrete attributes.
        return """
     <attribute key="%s" value="%s" type="%s"/>
    """ % (key, valueRepr, typeRepr)

    # create - assign - start - complete
    def gen(self, t, attrs):
        times = self.hook(t, attrs)
        sformat = "%Y-%m-%dT%H:%M:%S.000+01:00"
        formattimes = [time.strftime(sformat, time.gmtime(t)) for t in times]
        attr_str = ""
        for attr in attrs.keys():
            attr_str += self.formatAttr(attr, attrs[attr])
        actions = ["create", "assign", "start", "complete"]
        res = ""
        for (t,a) in zip(formattimes, actions):
            res += """ 
            <event>
              %s
              <string key="org:resource" value="Guancio"/>
              <string key="lifecycle:transition" value="%s"/>
              <string key="concept:name" value="%s"/>
              <date key="time:timestamp" value="%s"/>
              </event>""" % (attr_str,a, self.name, t)

        return (res, times[-1])

class Recursion:
    def __init__(self, e):
        self.e = e
    def gen(self, t, attrs):
        res = ""
        p = 0.5
        max_rec = 2
        it = 1
        while random.random() < p:
            if max_rec is not None and it > max_rec:
                break
            (res1, t) = self.e.gen(t, attrs)
            res += res1
            it += 1
        return (res, t)
    
class Choice:
    def __init__(self, l, hook=None):
        self.l = l
        self.hook = hook
        if (self.hook is None):
            self.hook = defaultChoiceHook
    def gen(self, t, attrs):
        res = ""
        task = self.hook(self.l, attrs)
        (res1, t1) = task.gen(t, attrs)
        return (res1, t1)

class Par:
    def __init__(self, l):
        self.l = l
    def gen(self, t, attrs):
        l1 = self.l[:]
        res = ""
        t1 = t
        while len(l1) > 0:
            x = random.randint(0, len(l1)-1)
            x = l1.pop(x)
            (res1, t2) = x.gen(t, attrs)
            # Non vero parallelo
            # t = t1
            t1 = max(t1, t2)
            res += res1
        return (res, t1)

class Sequence:
    def __init__(self, l, p=0):
        self.l = l
        self.p = p
    def gen(self, t, attrs):
        res = ""
       # exceptions = [Entry("Draft")]#, Entry("Study")]
        for a in self.l:
            (res1, t) = a.gen(t, attrs)
            res += res1
          #  if random.random() < self.p:
           #     a1 = exceptions[random.randint(0, len(exceptions) - 1)]
            #    (res1, t) =a1.gen(t)
             #   res += res1
        return (res, t)
        

	
def generate_instance(process, i):
    t = time.time() + 3600 * i
    return """
	<trace>
		<string key="concept:name" value="%d"/>
		<string key="description" value="PaperOne"/>
                %s
        </trace>
""" % (i, process.gen(t,{})[0])


def generate(process, instances):
    print("""<?xml version="1.0" encoding="UTF-8" ?>
        <log xes.version="1.0" xes.features="nested-attributes" openxes.version="1.0RC7" xmlns="http://www.xes-standard.org/">
	<extension name="Lifecycle" prefix="lifecycle" uri="http://www.xes-standard.org/lifecycle.xesext"/>
	<extension name="Organizational" prefix="org" uri="http://www.xes-standard.org/org.xesext"/>
	<extension name="Time" prefix="time" uri="http://www.xes-standard.org/time.xesext"/>
	<extension name="Concept" prefix="concept" uri="http://www.xes-standard.org/concept.xesext"/>
	<extension name="Semantic" prefix="semantic" uri="http://www.xes-standard.org/semantic.xesext"/>
	<global scope="trace">
		<string key="concept:name" value="__INVALID__"/>
	</global>
	<global scope="event">
		<string key="concept:name" value="__INVALID__"/>
		<string key="lifecycle:transition" value="complete"/>
	</global>
	<classifier name="MXML Legacy Classifier" keys="concept:name lifecycle:transition"/>
	<classifier name="Event Name" keys="concept:name"/>
	<classifier name="Resource" keys="org:resource"/>
	<string key="source" value="Example One from Guancio"/>
	<string key="concept:name" value="Paper"/>
	<string key="lifecycle:model" value="standard"/>
	<string key="description" value="Paper Submission"/>
       """)

    for i in range(instances):
        print(generate_instance(process, i))
    
    print("""
        </log>
          """)

