package gui;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import java.lang.reflect.Method;
import java.util.ArrayList;

//Modified from http://java-sl.com/tip_merge_documents.html
public class TextPaneHack {
    private static int nodeCount;
    private static int nodeLimit;

    public static void copyDocumentTillSpecIndex(DefaultStyledDocument source, DefaultStyledDocument dest, int specIndex) throws BadLocationException {
        nodeCount=0;
        nodeLimit=specIndex;

        ArrayList<DefaultStyledDocument.ElementSpec> specs=new ArrayList<>();
        DefaultStyledDocument.ElementSpec spec=new DefaultStyledDocument.ElementSpec(new SimpleAttributeSet(),
                DefaultStyledDocument.ElementSpec.EndTagType);
        specs.add(spec);
        fillSpecsLimited(source.getDefaultRootElement(), specs, false);
        spec=new DefaultStyledDocument.ElementSpec(new SimpleAttributeSet(), DefaultStyledDocument.ElementSpec.StartTagType);
        specs.add(spec);

        DefaultStyledDocument.ElementSpec[] arr = new DefaultStyledDocument.ElementSpec[specs.size()];
        specs.toArray(arr);
        insertSpecs(dest, dest.getLength(), arr);
    }

    protected static void insertSpecs(DefaultStyledDocument doc, int offset, DefaultStyledDocument.ElementSpec[] specs) {
        try {
            //doc.insert(0, specs);  method is protected so we have to
            //extend document or use such a hack
            Method m=DefaultStyledDocument.class.getDeclaredMethod("insert", int.class, DefaultStyledDocument.ElementSpec[].class);
            m.setAccessible(true);
            m.invoke(doc, offset, specs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void fillSpecsLimited(Element elem, ArrayList<DefaultStyledDocument.ElementSpec> specs, boolean includeRoot) throws BadLocationException{
        DefaultStyledDocument.ElementSpec spec;

        if(++nodeCount>nodeLimit) return;

        if (elem.isLeaf()) {
            String str=elem.getDocument().getText(elem.getStartOffset(), elem.getEndOffset()-elem.getStartOffset());
            spec=new DefaultStyledDocument.ElementSpec(elem.getAttributes(),
                    DefaultStyledDocument.ElementSpec.ContentType,str.toCharArray(), 0, str.length());
            specs.add(spec);
        }
        else {
            if (includeRoot) {
                spec=new DefaultStyledDocument.ElementSpec(elem.getAttributes(), DefaultStyledDocument.ElementSpec.StartTagType);
                specs.add(spec);
            }
            for (int i=0; i<elem.getElementCount(); i++) {
                fillSpecsLimited(elem.getElement(i), specs, true);
            }

            if (includeRoot) {
                spec=new DefaultStyledDocument.ElementSpec(elem.getAttributes(), DefaultStyledDocument.ElementSpec.EndTagType);
                specs.add(spec);
            }
        }
    }

    //public static int specCount(Element elem){}

    public static int specCount(Element elem, boolean includeRoot){

        if (elem.isLeaf()) {
            return 1;
        }
        else {
            int count = (includeRoot?2:0);

            for (int i=0; i<elem.getElementCount(); i++) {
                count += specCount(elem.getElement(i), true);
            }

            return count;
        }
    }
}
