#!/usr/bin/python
import random
import time

def formatAttr(key, value):
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

def defaultHook(startTime, attrs):
    t = startTime
    delta1 = random.randint(1, 30*60)
    delta2 = random.randint(delta1+1, 59*60)
    t0 = t+delta1
    t1 = t+delta2
    return t0, t1, ""
    
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
            self.hook = defaultHook
    def gen(self, t, attrs):
        t0, t1, data = self.hook(t, attrs)
        tf0 = time.strftime("%Y-%m-%dT%H:%M:%S.000+01:00", time.gmtime(t0))
        tf1 = time.strftime("%Y-%m-%dT%H:%M:%S.000+01:00", time.gmtime(t1))
        res = """
        <event>
          %s
          <string key="org:resource" value="Guancio"/>
          <string key="lifecycle:transition" value="start"/>
          <string key="concept:name" value="%s"/>
          <date key="time:timestamp" value="%s"/>
          </event>
        <event>
          <string key="org:resource" value="Guancio"/>
          <string key="lifecycle:transition" value="complete"/>
          <string key="concept:name" value="%s"/>
          <date key="time:timestamp" value="%s"/>
        </event>
        """ % (data, self.name, tf0, self.name, tf1)

        return (res, t1)

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

def defaultChoiceHook(l, attr):
    x = random.randint(0, len(l)-1)
    return l[x]
    
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
        


def requestHook(t, attrs):
    dests = ["Italy", "UK", "USA", "Spain"]
    dest = dests[random.randint(0, len(dests)-1)]
    attrs["dest"] = dest

    attrs["age"] = random.randint(18, 80)

    t0, t1, data = defaultHook(t, attrs)
    data = ""
    for attr in attrs.keys():
        data += """
    <Attribute name="%s">%s</Attribute>
    """%(attr, attrs[attr])
    return t0,t1,data

def carHook(t, attrs):
    t0, t1, data = defaultHook(t, attrs)
    if attrs["dest"] == "Italy":
        t1 += 0
    elif attrs["dest"] == "Spain" or attrs["dest"] == "USA":
        t1 += random.randint(1, 60 * 60)
    else:
        t1 += random.randint(0.5 * 60 * 60, 3 * 60 * 60)
    return t0,t1,data

def airHook(t, attrs):
    t0, t1, data = defaultHook(t, attrs)
    if attrs["dest"] == "Italy":
        t1 += 0
    elif attrs["dest"] == "Spain" or attrs["dest"] == "UK":
        t1 += random.randint(1, 60 * 60)
    else:
        t1 += random.randint(60 * 60, 5 * 60 * 60)
    return t0,t1,data

def driveHook(t, attrs):
    t0, t1, data = defaultHook(t, attrs)
    return t0,t1 + random.randint(1, 60 * 60),data

def choiceHook(l, attrs):
    if 30 < attrs["age"] < 60:
        return l[1]
    return l[0]

def notifyHook(t, attrs):
    # Urgente = ["Yes", "No"]
    # Urgente = [True, False]
    # Urgente = [0, 1]
    Urgente = [0.0, 1.0]
    urg = Urgente[random.randint(0, len(Urgente)-1)]
    attrs["urg"] = urg


    t0, t1, data = defaultHook(t, attrs)
    data = ""
    for attr in attrs.keys():
        data += formatAttr(attr, attrs[attr])
    return t0,t1,data

def choiceHook(l, attrs):
    if  attrs["urg"] == "Yes":
        return l[1]
    return l[0]

	
def gen_sequence(t):
    acts = Sequence([ Entry("NotifyBug", notifyHook),
                      Choice([
                          Sequence([ Entry("CheckBug"),
                                     Entry("FixBug" )
                               ]),
                          Entry("FixBug")
                          ], choiceHook)
                      ])
                      
    return acts.gen(t,{})[0]	     


def gen_log(i):
    t = time.time() + 3600 * i
    return """
	<trace>
		<string key="concept:name" value="%d"/>
		<string key="description" value="PaperOne"/>
                %s
        </trace>
""" % (i, gen_sequence(t))


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

for i in range(500):
    print(gen_log(i))

print("""
</log>
""")

